package com.theoryinpractise.halbuilder.impl;

public class ContentType {
    private String type;
    private String subType;

    public ContentType(String contentType) {
        String[] parts = contentType.split("/");
        type = parts[0];
        subType = parts[1];
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContentType that = (ContentType) o;

        if (!subType.equals(that.subType)) return false;
        if (!type.equals(that.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + subType.hashCode();
        return result;
    }

    public boolean matches(String contentType) {
        return matches(new ContentType(contentType));
    }

    public boolean matches(ContentType contentType) {

        if (typeMatches(getType(), contentType.getType())) {
            if (typeMatches(getSubType(), contentType.getSubType())) {
                return true;
            }
        }

        return false;

    }

    private boolean typeMatches(String left, String right) {
        return (left.equals(right) || "*".equals(right));
    }

}
