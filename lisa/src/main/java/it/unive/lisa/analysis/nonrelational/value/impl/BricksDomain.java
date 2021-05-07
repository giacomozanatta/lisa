package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class BricksDomain extends BaseNonRelationalValueDomain<BricksDomain> {


    public List<Brick> padList(List<Brick> list1, List<Brick> list2) {
        // if list2 is smaller than list1 -> return list1
        if (list2.size() <= list1.size()) {
            return list1;
        }
        int sizeDiff = list2.size() - list1.size();
        List<Brick> paddedList = new ArrayList<Brick>();
        int emptyBricksAdded = 0;
        int j = 0;

        for (int i = 0; i < list2.size() - 1; i++) {
            if (emptyBricksAdded >= sizeDiff || !(j == list1.size() || (list1.get(i) == list2.get(j)))) {
                paddedList.add(list1.get(j));
                j++; // remove head
            } else {
                paddedList.add(new Brick(new TreeSet<String>(), 0,0)); // add empty bricks
                emptyBricksAdded++;
            }
        }

        return paddedList;
    }


    public int compareLists(List<Brick> list1, List<Brick> list2) {
        if ((list2.size() == 1 && list2.get(0) instanceof TopBrick) || (list1.size() == 0)) {
            return 1;
        }
        List<Brick> L1 = padList(list1, list2);
        List<Brick> L2 = padList(list2, list1);

        for (int i = 0; i < L2.size(); i++) {
            if (L1.get(i).compareTo(L2.get(i)) > 0 ) {
                // brick i of L1 is bigger than brick i of L2
                return -1;
            }
        }
        return 1;
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