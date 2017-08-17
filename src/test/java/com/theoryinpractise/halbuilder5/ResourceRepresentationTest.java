package com.theoryinpractise.halbuilder5;

import com.damnhandy.uri.template.UriTemplate;
import com.jayway.jsonpath.JsonPath;
import com.theoryinpractise.halbuilder5.json.JsonRepresentationReader;
import com.theoryinpractise.halbuilder5.json.JsonRepresentationWriter;
import io.vavr.Function1;
import io.vavr.Function2;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import okio.ByteString;
import org.testng.annotations.Test;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.theoryinpractise.halbuilder5.Support.defaultObjectMapper;
import static com.theoryinpractise.halbuilder5.json.JsonRepresentationReader.readByteStringAs;

public class ResourceRepresentationTest {

  private String noOp(String event) {
    return "";
  }

  @Test
  public void testEmptyRepresentationIsEmpty() {
    assertThat(ResourceRepresentation.empty().isEmpty()).isTrue();
  }

  @Test
  public void testEmptyRepresentationRendersJsonCorrectly() {
    ResourceRepresentation<?> resource = ResourceRepresentation.empty().withLink("foo", "/foo");
    JsonRepresentationWriter.create().print(resource);
  }

  @Test
  public void testNonEmptyRepresentationIsNotEmpty() {
    assertThat(ResourceRepresentation.create("value").isEmpty()).isFalse();
  }

  @Test
  public void testMultipleEmbeddedRepresentations() {

    Account account = Account.of("0101232", "Test Account");

    ResourceRepresentation<Account> accountRep =
        ResourceRepresentation.create("/somewhere", account)
            .withLink(
                "bible-verse",
                "https://www.bible.com/bible/1/mat.11.28",
                HashMap.of("content-type", "text/html"));

    Account subAccountA = Account.of("87912312-a", "Sub Account A");
    ResourceRepresentation<Account> subAccountRepA =
        ResourceRepresentation.create("/subaccount/a", subAccountA);

    Account subAccountB = Account.of("87912312-b", "Sub Account B");
    ResourceRepresentation<Account> subAccountRepB =
        ResourceRepresentation.create("/subaccount/a", subAccountB);

    accountRep = accountRep.withRepresentation("bank:associated-account", subAccountRepA);
    accountRep = accountRep.withRepresentation("bank:associated-account", subAccountRepB);

    JsonRepresentationWriter jsonRepresentationWriter = JsonRepresentationWriter.create();

    String representation = jsonRepresentationWriter.print(accountRep).utf8();

    System.out.println(representation);

    String accountNumberPath = "$['_embedded']['bank:associated-account'][1]['accountNumber']";
    String accountNumber = JsonPath.parse(representation).read(accountNumberPath);

    assertThat(accountNumber).isEqualTo("87912312-b");
  }

  @Test
  public void testBasicRepresentationUsage() {

    Account account = Account.of("0101232", "Test Account");

    ResourceRepresentation<Account> accountRep =
        ResourceRepresentation.create("/somewhere", account)
            .withLink(
                "bible-verse",
                "https://www.bible.com/bible/1/mat.11.28",
                HashMap.of("content-type", "text/html"));

    accountRep.getLinks().forEach(link -> System.out.println(" *** " + link.toString()));

    assertThat(accountRep.get().name()).isEqualTo("Test Account");

    ResourceRepresentation<String> nameRep = accountRep.map(Account::name);
    assertThat(nameRep.get()).isEqualTo("Test Account");

    int lengthOfName = accountRep.transform(a -> a.name().length());

    assertThat(lengthOfName).isEqualTo(12);

    Account subAccount = Account.of("87912312", "Sub Account");
    ResourceRepresentation<Account> subAccountRep =
        ResourceRepresentation.create("/subaccount", subAccount);

    ResourceRepresentation<Account> accountRepWithLinks =
        accountRep.withRepresentation("bank:associated-account", subAccountRep);

    JsonRepresentationWriter jsonRepresentationWriter = JsonRepresentationWriter.create();

    ByteString representation = jsonRepresentationWriter.print(accountRepWithLinks);

    System.out.println(representation.utf8());

    ResourceRepresentation<ByteString> byteStringResourceRepresentation =
        JsonRepresentationReader.create(defaultObjectMapper())
            .read(new StringReader(representation.utf8()));

    ResourceRepresentation<Map> readRepresentation =
        byteStringResourceRepresentation.map(
            readByteStringAs(defaultObjectMapper(), Map.class, () -> Collections.emptyMap()));

    assertWithMessage("read representation should not be null")
        .that(readRepresentation)
        .isNotNull();
    Map<String, Object> readValue = readRepresentation.get();
    assertWithMessage("account name should be Test Account")
        .that(readValue.get("name"))
        .isEqualTo("Test Account");

    ResourceRepresentation<Account> readAccountRepresentation =
        byteStringResourceRepresentation.map(
            readByteStringAs(defaultObjectMapper(), Account.class, () -> Account.of("", "")));

    assertWithMessage("read representation should not be null")
        .that(readRepresentation)
        .isNotNull();
    Account readAccount = readAccountRepresentation.get();
    assertWithMessage("account name should be Test Account")
        .that(readAccount.name())
        .isEqualTo("Test Account");

    Option<String> subLink =
        accountRepWithLinks
            .getResourcesByRel("bank:associated-account")
            .headOption()
            .flatMap(ResourceRepresentation::getResourceLink)
            .map(Links::getHref);

    System.out.println(subLink);

    Function1<String, String> deleteFunction =
        linkFunction(accountRepWithLinks, "self", this::deleteResource);

    Function2<ResourceRepresentation<?>, String, String> deleteRepFunction =
        repFunction("self", this::deleteResource);

    System.out.println(deleteFunction.apply("click-event"));
    System.out.println(deleteRepFunction.apply(accountRepWithLinks, "click-event-on-rep"));

    UriTemplate template =
        UriTemplate.buildFromTemplate("http://api.smxemail.com/api/sp")
            .literal("/mailbox")
            .path("mailbox")
            .literal("/updateAlias")
            .build();

    System.out.println(template.getTemplate());

    System.out.println(
        template.expand(HashMap.<String, Object>of("mailbox", "greg@amer.com").toJavaMap()));
  }

  private String deleteResource(Link link, String event) {
    System.out.printf("Deleting %s due to %s\n", Links.getHref(link), event);
    return String.format("deleted %s", Links.getHref(link));
  }

  Function2<ResourceRepresentation<?>, String, String> repFunction(
      String rel, Function2<Link, String, String> fn) {
    return (rep, event) ->
        rep.getLinkByRel(rel).map(link -> fn.apply(link, event)).getOrElse(() -> noOp(event));
  }

  Function1<String, String> linkFunction(
      ResourceRepresentation<?> rep, String rel, Function2<Link, String, String> fn) {
    return rep.getLinkByRel(rel).map(link -> fn.curried().apply(link)).getOrElse(this::noOp);
  }
}
