package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;

public class BricksDomain extends BaseNonRelationalValueDomain<BricksDomain> {

    @Override
    protected BricksDomain lubAux(BricksDomain other) throws SemanticException {
        return null;
    }

    @Override
    protected BricksDomain wideningAux(BricksDomain other) throws SemanticException {
        return null;
    }

    @Override
    protected boolean lessOrEqualAux(BricksDomain other) throws SemanticException {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public BricksDomain top() {
        return null;
    }

    @Override
    public BricksDomain bottom() {
        return null;
    }

    @Override
    public String representation() {
        return null;
    }
}