package com.theoryinpractise.halbuilder;

import com.google.common.collect.Iterables;
import com.google.common.truth.BooleanSubject;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.Representation;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;
import static java.util.Objects.requireNonNull;

public class RepresentationSubject extends Subject {

  @NullableDecl private final Representation actual;

  public static RepresentationSubject assertThatRepresentation(Representation rep) {
    return assertAbout(REPRESENTATION_SUBJECT_FACTORY).that(rep);
  }

  private static final Factory<RepresentationSubject, Representation> REPRESENTATION_SUBJECT_FACTORY = RepresentationSubject::new;

  private RepresentationSubject(FailureMetadata failureMetadata, @NullableDecl Representation subject) {
    super(failureMetadata, subject);
    this.actual = subject;
  }

  public void containsRel(String rel) {
    List<Link> links = requireNonNull(actual).getLinks();
    checkLinkRels("getLinks()", links, rel).isTrue();
  }

  public void containsRel(String lookup, String rel) {
    List<Link> links = requireNonNull(actual).getLinksByRel(lookup);
    checkLinkRels("getLinksByRel()", links, rel).isTrue();
  }

  public void doesNotContainRel(String lookup, String rel) {
    List<Link> links = requireNonNull(actual).getLinksByRel(lookup);
    checkLinkRels("getLinksByRel()", links, rel).isFalse();
  }

  private BooleanSubject checkLinkRels(String format, List<Link> links, String rel) {
    boolean hasMatch = false;
    for (Object object : links) {
      Link link = (Link) object;
      if (rel.equals(link.getRel()) || Iterables.contains(WHITESPACE_SPLITTER.split(link.getRel()), rel)) {
        hasMatch = true;
      }
    }
    return check(format).that(hasMatch);
  }
}
