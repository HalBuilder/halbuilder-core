package com.theoryinpractise.halbuilder5;

import com.damnhandy.uri.template.UriTemplate;
import com.jayway.jsonpath.JsonPath;
import com.theoryinpractise.halbuilder5.json.JsonRepresentationReader;
import com.theoryinpractise.halbuilder5.json.JsonRepresentationWriter;
import io.vavr.collection.List;
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
  public void testRepresentationLinks() {
    assertThat(
            ResourceRepresentation.create("value")
                .withLink("link", "/link")
                .getLinkByRel("link")
                .map(Links::getHref)
                .get())
        .isEqualTo("/link");

    assertThat(
            ResourceRepresentation.create("/self", "value")
                .withLinks(
                    List.of(Links.create("link1", "/link1"), Links.create("link2", "/link2")))
                .getLinkByRel(ResourceRepresentation.SELF)
                .map(Links::getHref)
                .get())
        .isEqualTo("/self");
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

    String accountNumber =
        jsonPath(representation, "$['_embedded']['bank:associated-account'][1]['accountNumber']");

    assertThat(accountNumber).isEqualTo("87912312-b");
  }

  private String jsonPath(String json, String path) {
    return JsonPath.parse(json).read(path);
  }

  @Test
  public void testEmbeddedRepresentationNaturalOrdering() {
    ResourceRepresentation<?> resource =
        ResourceRepresentation.empty()
            .withLink("foo", "/foo/1")
            .withRepresentation("foo", ResourceRepresentation.empty().withLink("self", "/foo/1"))
            .withLink("foo", "/foo/2")
            .withRepresentation("foo", ResourceRepresentation.empty().withLink("self", "/foo/2"))
            .withLink("foo", "/foo/3")
            .withRepresentation("foo", ResourceRepresentation.empty().withLink("self", "/foo/3"));

    JsonRepresentationWriter jsonRepresentationWriter = JsonRepresentationWriter.create();
    String representation = jsonRepresentationWriter.print(resource).utf8();

    System.out.println("java map");
    System.out.println(resource.getResources().toJavaMap());

    System.out.println("vavr list");
    System.out.println(resource.getResources().toList());

    System.out.println("vavr multimap");
    System.out.println(resource.getResources());

    System.out.println("vavr multimap->foo");
    System.out.println(resource.getResources().get("foo"));

    System.out.println(representation);

    assertThat(jsonPath(representation, "$['_links']['foo'][0]['href']")).isEqualTo("/foo/1");
    assertThat(jsonPath(representation, "$['_links']['foo'][1]['href']")).isEqualTo("/foo/2");
    assertThat(jsonPath(representation, "$['_links']['foo'][2]['href']")).isEqualTo("/foo/3");

    assertThat(jsonPath(representation, "$['_embedded']['foo'][0]['_links']['self']['href']"))
        .isEqualTo("/foo/1");
    assertThat(jsonPath(representation, "$['_embedded']['foo'][1]['_links']['self']['href']"))
        .isEqualTo("/foo/2");
    assertThat(jsonPath(representation, "$['_embedded']['foo'][2]['_links']['self']['href']"))
        .isEqualTo("/foo/3");
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
