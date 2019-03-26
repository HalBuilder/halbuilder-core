module com.theoryinpractise.halbuilder.core {
    exports com.theoryinpractise.halbuilder;
    exports com.theoryinpractise.halbuilder.impl;
    exports com.theoryinpractise.halbuilder.impl.api;
    exports com.theoryinpractise.halbuilder.impl.bytecode;
    exports com.theoryinpractise.halbuilder.impl.representations;
    requires com.google.common;
    requires jsr305; // FIXME filename-based module
    requires transitive com.theoryinpractise.halbuilder.api;
}
