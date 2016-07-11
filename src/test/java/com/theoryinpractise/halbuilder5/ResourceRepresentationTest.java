package com.theoryinpractise.halbuilder5;

import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.theoryinpractise.halbuilder5.json.JsonRepresentationReader;
import com.theoryinpractise.halbuilder5.json.JsonRepresentationWriter;
import javaslang.Function1;
import javaslang.Function2;
import javaslang.control.Option;
import javaslang.collection.HashMap;
import okio.ByteString;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class ResourceRepresentationTest {

  private static ObjectMapper objectMapper = getObjectMapper();

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);
    return objectMapper;
  }

  private String noOp(String event) {
    return "";
  }

  @Test
  public void testBasicRepresentationUsage() throws IOException {

    Account account = Account.of("0101232", "Test Account");

    ResourceRepresentation<Account> accountRep =
        ResourceRepresentation.create("/somewhere", account)
            .withLink("bible-verse", "https://www.bible.com/bible/1/mat.11.28", HashMap.of("content-type", "text/html"));

    accountRep.getLinks(false).forEach(link -> System.out.println(" *** " + link.toString()));

    assertThat(accountRep.get().name()).isEqualTo("Test Account");

    ResourceRepresentation<String> nameRep = accountRep.map(Account::name);
    assertThat(nameRep.get()).isEqualTo("Test Account");

    int lengthOfName = accountRep.transform(a -> a.name().length());

    assertThat(lengthOfName).isEqualTo(12);

    Account subAccount = Account.of("87912312", "Sub Account");
    ResourceRepresentation<Account> subAccountRep = ResourceRepresentation.create("/subaccount", subAccount);

    ResourceRepresentation<Account> accountRepWithLinks = accountRep.withRepresentation("bank:associated-account", subAccountRep);

    ByteString representation = JsonRepresentationWriter.create(objectMapper).print(accountRepWithLinks);

    System.out.println(representation.utf8());

    ResourceRepresentation<ByteString> byteStringResourceRepresentation =
        new JsonRepresentationReader().read(new StringReader(representation.utf8()));

    ResourceRepresentation<Map> readRepresentation =
        byteStringResourceRepresentation.map(uncheckedObjectMap(Map.class, Collections.emptyMap()));

    assertWithMessage("read representation should not be null").that(readRepresentation).isNotNull();
    Map<String, Object> readValue = readRepresentation.get();
    assertWithMessage("account name should be Test Account").that(readValue.get("name")).isEqualTo("Test Account");

    ResourceRepresentation<Account> readAccountRepresentation =
        byteStringResourceRepresentation.map(uncheckedObjectMap(Account.class, Account.of("", "")));

    assertWithMessage("read representation should not be null").that(readRepresentation).isNotNull();
    Account readAccount = readAccountRepresentation.get();
    assertWithMessage("account name should be Test Account").that(readAccount.name()).isEqualTo("Test Account");

    Option<String> subLink =
        accountRepWithLinks
            .getResourcesByRel("bank:associated-account")
            .headOption()
            .flatMap(ResourceRepresentation::getResourceLink)
            .map(Links::getHref);

    System.out.println(subLink);

    Function1<String, String> deleteFunction = linkFunction(accountRepWithLinks, "self", this::deleteResource);

    Function2<ResourceRepresentation<?>, String, String> deleteRepFunction = repFunction("self", this::deleteResource);

    System.out.println(deleteFunction.apply("click-event"));
    System.out.println(deleteRepFunction.apply(accountRepWithLinks, "click-event-on-rep"));

    UriTemplate template =
        UriTemplate.buildFromTemplate("http://api.smxemail.com/api/sp")
            .literal("/mailbox")
            .path("mailbox")
            .literal("/updateAlias")
            .build();

    System.out.println(template.getTemplate());

    System.out.println(template.expand(ImmutableMap.of("mailbox", "greg@amer.com")));
  }

  public <T> Function<ByteString, T> uncheckedObjectMap(Class<T> classType, T defaultValue) {
    return bs -> {
      try {
        return objectMapper.readValue(bs.utf8(), classType);
      } catch (IOException e) {
        e.printStackTrace();
        return defaultValue;
      }
    };
  }

  private String deleteResource(Link link, String event) {
    System.out.printf("Deleting %s due to %s\n", Links.getHref(link), event);
    return String.format("deleted %s", Links.getHref(link));
  }

  Function2<ResourceRepresentation<?>, String, String> repFunction(String rel, Function2<Link, String, String> fn) {
    return (rep, event) -> rep.getLinkByRel(rel).map(link -> fn.apply(link, event)).getOrElse(() -> noOp(event));
  }

  Function1<String, String> linkFunction(ResourceRepresentation<?> rep, String rel, Function2<Link, String, String> fn) {
    return rep.getLinkByRel(rel).map(link -> fn.curried().apply(link)).getOrElse(this::noOp);
  }
}
