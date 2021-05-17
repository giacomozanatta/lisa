package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;

import java.util.*;

public class BricksDomain extends BaseNonRelationalValueDomain<BricksDomain> {
    protected List<Brick> bricks;

    public BricksDomain(List<Brick> bricks) {
        super();
        this.bricks = bricks;
    }


    @Override
    protected BricksDomain evalTypeConv(BinaryExpression conv, BricksDomain left, BricksDomain right) {
        return super.evalTypeConv(conv, left, right);
    }

    @Override
    protected BricksDomain evalTypeCast(BinaryExpression cast, BricksDomain left, BricksDomain right) {
        return super.evalTypeCast(cast, left, right);
    }

    @Override
    protected BricksDomain evalNullConstant(ProgramPoint pp) {
        return super.evalNullConstant(pp);
    }

    @Override
    protected BricksDomain evalNonNullConstant(Constant constant, ProgramPoint pp) {
        if (constant.getValue() instanceof String) {
            List<Brick> bricks = new ArrayList<>();
            bricks.add(new Brick(Collections.singleton((String) constant.getValue()), 1,1));
            return new BricksDomain(bricks);
        }
        return super.evalNonNullConstant(constant, pp);
    }

    @Override
    protected BricksDomain evalUnaryExpression(UnaryOperator operator, BricksDomain arg, ProgramPoint pp) {
        // non dovrebbero esistere (?)
        return super.evalUnaryExpression(operator, arg, pp);
    }

    @Override
    protected BricksDomain evalBinaryExpression(BinaryOperator operator, BricksDomain left, BricksDomain right, ProgramPoint pp) {
        // STRING_CONCAT --- STRING_EQUALS (?)
        return super.evalBinaryExpression(operator, left, right, pp);
    }

    @Override
    protected BricksDomain evalTernaryExpression(TernaryOperator operator, BricksDomain left, BricksDomain middle, BricksDomain right, ProgramPoint pp) {
        // STRING_REPLACE --- STRING_SUBSTRING
        return super.evalTernaryExpression(operator, left, middle, right, pp);
    }

    public List<Brick> padList(List<Brick> list1, List<Brick> list2) {
        // if list2 is smaller than list1 -> return list1
        if (list2.size() <= list1.size()) {
            return list1;
        }
        int sizeDiff = list2.size() - list1.size();
        List<Brick> paddedList = new ArrayList<>();
        int emptyBricksAdded = 0;
        int j = 0;

        for (int i = 0; i < list2.size() - 1; i++) {
            if (emptyBricksAdded >= sizeDiff || !(j == list1.size() || (list1.get(i) == list2.get(j)))) {
                paddedList.add(list1.get(j));
                j++; // remove head
            } else {
                paddedList.add(new Brick(new TreeSet<>(), 0,0)); // add empty bricks
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
       List<Brick> L1 = padList(bricks, other.bricks);
       List<Brick> L2 = padList(other.bricks, bricks);
       List<Brick> lubElement = new ArrayList<>();
       for (int i = 0; i < L1.size(); i++) {
           lubElement.add(L1.get(i).lub(L2.get(i)));
       }
       return new BricksDomain(lubElement);
    }

    @Override
    protected BricksDomain wideningAux(BricksDomain other) throws SemanticException {
        return null;
    }

    @Override
    protected boolean lessOrEqualAux(BricksDomain other) throws SemanticException {
        if ((other.bricks.size() == 1 && other.bricks.get(0) instanceof TopBrick) || (bricks.size() == 0)) {
            return false;
        }
        List<Brick> L1 = padList(bricks, other.bricks);
        List<Brick> L2 = padList(other.bricks, bricks);
        for (int i = 0; i < L2.size(); i++) {
            if (L1.get(i).compareTo(L2.get(i)) > 0 ) {
                // brick i of L1 is bigger than brick i of L2
                return false;
            }
        }
        return true;
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
        List<Brick> top = new ArrayList<>();
        top.add(new TopBrick());
        return new BricksDomain(top);
    }

    @Override
    public BricksDomain bottom() {
        return new BricksDomain(new ArrayList<>());
    }

    @Override
    public String representation() {
        return null;
    }
}