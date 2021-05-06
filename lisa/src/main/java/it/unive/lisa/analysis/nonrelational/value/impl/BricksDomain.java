package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class BricksDomain extends BaseNonRelationalValueDomain<BricksDomain> {


    public List<Brick> padList(List<Brick> list1, List<Brick> list2) {
        boolean list1smaller = list1.size() < list2.size();
        List<Brick> shorter = list1smaller ? list1 : list2;
        List<Brick> larger = list1smaller ? list2 : list1;
        int sizeDiff = Math.abs(larger.size() - shorter.size());
        List<Brick> paddedList = new ArrayList<Brick>();
        int emptyBricksAdded = 0;
        int j = 0;

        for (int i = 0; i < larger.size() - 1; i++) {
            if (emptyBricksAdded >= sizeDiff || !(j == shorter.size() || (shorter.get(i) == larger.get(j)))) {
                paddedList.add(shorter.get(j));
                j++; // remove head
            } else {
                paddedList.add(new Brick(new TreeSet<String>(), 0,0)); // add empty bricks
                emptyBricksAdded++;
            }
        }

        return paddedList;
    }




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
        return false;
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