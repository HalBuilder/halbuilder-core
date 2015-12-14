package com.theoryinpractise.halbuilder;

import com.google.common.collect.Iterables;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;
import com.theoryinpractise.halbuilder.api.Link;
import javaslang.collection.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;

public class LinkListSubject
    extends Subject<LinkListSubject, List<Link>> {

  private static final SubjectFactory<LinkListSubject, List<Link>> SUBJECT_FACTORY =
      new SubjectFactory<LinkListSubject, List<Link>>() {
        @Override
        public LinkListSubject getSubject(FailureStrategy fs, List<Link> target) {
          return new LinkListSubject(fs, target);
        }
      };

  public LinkListSubject(FailureStrategy failureStrategy, List<Link> subject) {
    super(failureStrategy, subject);
  }

  public static SubjectFactory<LinkListSubject, List<Link>> linkLists() {
    return SUBJECT_FACTORY;
  }

  public static LinkListSubject assertAboutLinkLists(List<Link> links) {
    return assertAbout(linkLists()).that(links);
  }

  public void containsRelCondition(String rel) {
    boolean hasMatch = false;
    for (Link link : getSubject()) {
      if (rel.equals(link.getRel()) || Iterables.contains(WHITESPACE_SPLITTER.split(link.getRel()), rel)) {
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
      if (rel.equals(link.getRel()) || Iterables.contains(WHITESPACE_SPLITTER.split(link.getRel()), rel)) {
        hasMatch = true;
      }
    }
    if (hasMatch) {
      fail("List contains rel: " + rel);
    }

  }

}
