package com.theoryinpractise.halbuilder5;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import io.vavr.collection.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.theoryinpractise.halbuilder5.Links.getRel;

public class LinkListSubject extends Subject<LinkListSubject, List<Link>> {

  private LinkListSubject(FailureMetadata failureMetadata, List<Link> subject) {
    super(failureMetadata, subject);
  }

  public static Subject.Factory<LinkListSubject, List<Link>> linkLists() {
    return LinkListSubject::new;
  }

  public static LinkListSubject assertThatLinkLists(List<Link> links) {
    return assertAbout(linkLists()).that(links);
  }

  public void containsRelCondition(String rel) {
    boolean hasMatch = false;
    for (Link link : getSubject()) {
      if (rel.equals(getRel(link))) {
        hasMatch = true;
      }
    }
    if (!hasMatch) {
      fail("List does not contain rel: " + rel);
    }
  }

  public void doesNotContainRelCondition(String rel) {
    boolean hasMatch = false;
    for (Link link : getSubject()) {
      if (rel.equals(getRel(link))) {
        hasMatch = true;
      }
    }
    if (hasMatch) {
      fail("List contains rel: " + rel);
    }
  }
}
