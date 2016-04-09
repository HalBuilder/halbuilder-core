package com.theoryinpractise.halbuilder5;

import com.theoryinpractise.halbuilder5.json.JsonRepresentationReader;
import com.theoryinpractise.halbuilder5.json.JsonRepresentationWriter;
import javaslang.Function1;
import javaslang.Function2;
import javaslang.collection.TreeMap;
import okio.ByteString;
import org.junit.Test;

import java.io.StringReader;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static javaslang.control.Option.some;

public class ResourceRepresentationTest {

  private final String noOp(String event) {
    return "";
  }

  @Test
  public void testBasicRepresentationUsage() {

    Account account = new Account("0101232", "Test Account");

    ResourceRepresentation<Account> accountRep = ResourceRepresentation.create("/somewhere", account);

    assertThat(accountRep.get().getName()).isEqualTo("Test Account");

    ResourceRepresentation<String> nameRep = accountRep.map(Account::getName);
    assertThat(nameRep.get()).isEqualTo("Test Account");

    int lengthOfName = accountRep.transform(a -> a.getName().length());

    assertThat(lengthOfName).isEqualTo(12);

    Account subAccount = new Account("87912312", "Sub Account");
    ResourceRepresentation<Account> subAccountRep = ResourceRepresentation.create("/subaccount", subAccount);

    ResourceRepresentation<Account> accountRepWithLinks = accountRep.withRepresentation("bank:associated-account", subAccountRep);

    ByteString representation = JsonRepresentationWriter.create().print(accountRepWithLinks);

    System.out.println(representation.utf8());

    ResourceRepresentation<TreeMap<String, Object>> readRepresentation =
        new JsonRepresentationReader().read(new StringReader(representation.utf8()));

    assertWithMessage("read representation should not be null").that(readRepresentation).isNotNull();
    TreeMap<String, Object> readValue = readRepresentation.get();
    assertWithMessage("account name should be Test Account").that(readValue.get("name")).isEqualTo(some("Test Account"));

    ResourceRepresentation<Account> readAccountRepresentation =
        new JsonRepresentationReader().read(Account.class, new StringReader(representation.utf8()));

    assertWithMessage("read representation should not be null").that(readRepresentation).isNotNull();
    Account readAccount = readAccountRepresentation.get();
    assertWithMessage("account name should be Test Account").that(readAccount.getName()).isEqualTo(some("Test Account"));

    //    Option<String> subLink =
    //        accountRepWithLinks
    //            .getResourcesByRel("bank:associated-account")
    //            .headOption()
    //            .flatMap(ResourceRepresentation::getResourceLink)
    //            .map(Links::getHref);
    //
    //    System.out.println(subLink);
    //
    //    Function1<String, String> deleteFunction = linkFunction(accountRepWithLinks, "self", this::deleteResource);
    //
    //    Function2<ResourceRepresentation<?>, String, String> deleteRepFunction = repFunction("self", this::deleteResource);
    //
    //    System.out.println(deleteFunction.apply("click-event"));
    //    System.out.println(deleteRepFunction.apply(accountRepWithLinks, "click-event-on-rep"));
    //
    //    UriTemplate template =
    //        UriTemplate.buildFromTemplate("http://api.smxemail.com/api/sp")
    //            .literal("/mailbox")
    //            .path("mailbox")
    //            .literal("/updateAlias")
    //            .build();
    //
    //    System.out.println(template.getTemplate());
    //
    //    System.out.println(template.expand(ImmutableMap.of("mailbox", "greg@amer.com")));
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
