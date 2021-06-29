package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;

import java.util.*;

public class BricksDomain extends BaseNonRelationalValueDomain<BricksDomain> {
    
	protected List<Brick> bricks;
	protected int kL = 3; // number of bricks in a list of bricks
	protected int kI = 3; // difference between max and min
	protected int kS = 3; // number of strings in a brick
    private Object data;

    public BricksDomain(List<Brick> bricks) {
        super();
        //this.bricks = bricks;
        this.bricks = Brick.normalize(bricks);
    }

    public BricksDomain(Object data) {
        this();
        this.data = data;
    }

    public BricksDomain() {
        super();
        this.bricks = new ArrayList<>();
    }

    public List<Brick> getBricks() {
		return bricks;
	}


	public void setBricks(List<Brick> bricks) {
		this.bricks = bricks;
	}

    public Object getData() {
        return data;
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
            String valString = constant.getValue().toString();
            bricks.add(new Brick(Collections.singleton(valString.substring(1, valString.length()-1)), 1,1));
            return new BricksDomain(bricks);
        }
        return new BricksDomain(constant.getValue());
       // return super.evalNonNullConstant(constant, pp);
    }

    @Override
    protected BricksDomain evalUnaryExpression(UnaryOperator operator, BricksDomain arg, ProgramPoint pp) {
        // non dovrebbero esistere (?)
        return super.evalUnaryExpression(operator, arg, pp);
    }

    @Override
    protected BricksDomain evalBinaryExpression(BinaryOperator operator, BricksDomain left, BricksDomain right, ProgramPoint pp) {
        // STRING_CONCAT --- STRING_EQUALS (?)
    	switch (operator) {
		case STRING_CONCAT:
			return stringConcatAux(left, right);			
		default:
			break;
		}
        return super.evalBinaryExpression(operator, left, right, pp);
    }

    @Override
    protected BricksDomain evalTernaryExpression(TernaryOperator operator, BricksDomain left, BricksDomain middle, BricksDomain right, ProgramPoint pp) {
        // STRING_REPLACE --- STRING_SUBSTRING
        switch (operator) {
            case STRING_REPLACE:
                return stringReplace(left, middle, right);
            case STRING_SUBSTRING:
                // get from program point the value of startPos and endPos
                int startPos = (Integer) left.getData();
                int endPos = (Integer) right.getData();
                return stringSubStr(left, startPos, endPos);
            default:
                break;
        }
        return super.evalTernaryExpression(operator, left, middle, right, pp);
    }

    public List<Brick> padList(List<Brick> list1, List<Brick> list2) {

        // if list2 is smaller than list1 -> return list1
        if (list2.size() <= list1.size()) {
            return list1;
        }
        System.out.println("*** PAD LIST ***");
        System.out.println("L1 before: " +  list1);
        System.out.println("L2 before: " + list2);
        int sizeDiff = list2.size() - list1.size();
        List<Brick> paddedList = new ArrayList<>();
        int emptyBricksAdded = 0;
        int j = 0;

        for (int i = 0; i < list2.size(); i++) {
            if (emptyBricksAdded >= sizeDiff) {
                paddedList.add(list1.get(j));
                j++; // remove head
            } else if ( j == list1.size() || (list1.get(j) == list2.get(i)) ) {
            	paddedList.add(new Brick(new TreeSet<>(), 0,0)); // add empty bricks
                emptyBricksAdded++;
            }            
            else {                
                paddedList.add(list1.get(j));
                j++;
            }
        }
        System.out.println("L1: " +  list1);
        System.out.println("L2: " + list2);
        System.out.println("L1 padded: " + paddedList);
        return paddedList;
    }
    
    @Override
    protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, BricksDomain left, BricksDomain right,
			ProgramPoint pp) {
    	
    	switch(operator){
    	case STRING_CONTAINS:
    		return stringContainsAux(left, right);
    	case STRING_ENDS_WITH:
    		return stringEndsAux(left, right);
    	case STRING_EQUALS:
    		return stringEqualsAux(left, right);
    	case STRING_STARTS_WITH:
    		return stringStartsAux(left, right);
    	default:
    		break;
    	}
		return Satisfiability.UNKNOWN;
	}

    @Override
    protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, BricksDomain left, BricksDomain middle, BricksDomain right, ProgramPoint pp) {
        return super.satisfiesTernaryExpression(operator, left, middle, right, pp);
    }

    @Override
    protected BricksDomain lubAux(BricksDomain other) throws SemanticException {
        System.out.println("*** LUB AUX ***");
        System.out.println("L1 before: " +  bricks);
        System.out.println("L2 before: " + other.bricks);
       List<Brick> L1 = padList(bricks, other.bricks);
       List<Brick> L2 = padList(other.bricks, bricks);

       List<Brick> lubElement = new ArrayList<>();
       for (int i = 0; i < L1.size(); i++) {
           lubElement.add(L1.get(i).lub(L2.get(i)));
       }
        System.out.println("L1: " +  L1);
        System.out.println("L2: " + L2);
       System.out.println("LUB: " + lubElement);
        System.out.println("\n");

       return new BricksDomain(lubElement);
    }

    @Override
    protected BricksDomain wideningAux(BricksDomain other) throws SemanticException {
        System.out.println("*** WIDENING AUX ***");

        List<Brick> l1 = this.bricks;
        List<Brick> l2 = other.bricks;
        System.out.println("L1 before: " +  l1);
        System.out.println("L2 before: " + l2);
        if( (!this.lessOrEqual(other) &&
        	!other.lessOrEqual(this)) ||
        		l1.size() > this.kL || 
        		l2.size() > this.kL)
        {
        	return this.top();
        }
        BricksDomain widen;
        if(l1.size()>l2.size()) {
            widen =  this.w(l2, l1);
        }
        else {
            widen =  this.w(l1, l2);
        }
        System.out.println("L1: " +  l1);
        System.out.println("L2: " + l2);
        System.out.println("WIDENING: " +  widen);
        System.out.println("\n\n");
        return widen;
        
    }

    @Override
    protected boolean lessOrEqualAux(BricksDomain other) throws SemanticException {
        if ((other.bricks.size() == 1 && other.bricks.get(0) instanceof TopBrick) || (bricks.size() == 0)) {
            return true;
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
    	/*
        String tmp = "";
        for( Brick br : this.bricks) {
        	tmp.concat(br.toString());
        }*/
        return bricks.toString();
    }
    
    /*
     * l2 is the longest list
     */
    private BricksDomain w(List<Brick> l1, List<Brick> l2) {
    	l1 = padList(l1, l2);
    	List<Brick> newList = new ArrayList<>();
    	for(int i = 0; i < l1.size(); i++) {
    	    Brick b = bNew(l1.get(i), l2.get(i));
    		newList.add(b);
    	}
    	// newList viene modificata dentro new BricksDomain (credo dal normalize)
        // -> se newList ha solo un TOPBRICK, dopo aver costruito il BricksDomain diventa vuoto.
    	BricksDomain newBrickDomain = new BricksDomain(newList);
    	System.out.println(newBrickDomain);
    	return newBrickDomain;
    }
    
    private Brick bNew(Brick b1, Brick b2) {
    	if(b1 instanceof TopBrick || b2 instanceof TopBrick) {
    		return new TopBrick();
    	}
    	Set<String> tmp = new TreeSet<String>();
    	tmp.addAll(b1.getStrings());
    	tmp.addAll(b2.getStrings());
    	
    	if( tmp.size() > this.kS ) {
    		return new TopBrick();
    	}
    	int M = Math.max(b1.getMax(), b2.getMax());
    	int m = Math.min(b1.getMin(), b2.getMin());
    	if(M-m > this.kI) {
    		return new Brick(tmp, 0);
    	}
    	return new Brick(tmp, m, M);
    }
    
    /*
     * Creates a new BricksDomain containing the concatenation of left and right
     */
    private BricksDomain stringConcatAux(BricksDomain left, BricksDomain right) {
    	List<Brick> l1 = left.getBricks();
    	List<Brick> l2 = right.getBricks();
    	List<Brick> concat = new ArrayList<Brick>();
    	concat.addAll(cloneOf(l1));
    	concat.addAll(cloneOf(l2));
    	return new BricksDomain(concat);
    }
    
    /*
     * left contains right?
     * Here we use two boolean flags to keep track of whether l1 contains or not contains all elements of l2 
     */
    private Satisfiability stringContainsAux(BricksDomain left, BricksDomain right) {
    	List<Brick> l1 = left.getBricks();
    	List<Brick> l2 = right.getBricks();
    	boolean contains = false;
    	boolean notContains = false;
    	for( Brick brick : l2) {
    		if(l1.contains(brick)) {
    			contains = true;
    		}else {
    			notContains = true;
    		}
    	}
    	// XOR operation
    	if (!contains ^ notContains) {
    		return Satisfiability.UNKNOWN;
    	}
    	else {
    		if(contains) {
    			return Satisfiability.SATISFIED;
    		}
    		else {
    			return Satisfiability.NOT_SATISFIED;
    		}
    	}
    }
    
    
    /*
     * left ends with right?
     * We assume that l2 is shorter than l1
     */ 
    private Satisfiability stringEndsAux(BricksDomain left, BricksDomain right) {
    	List<Brick> l1 = left.getBricks();
    	List<Brick> l2 = right.getBricks();
    	boolean ends = true;
    	if(l2.size()<=l1.size()) {    		
    		int j = l1.size()-1;
    		for(int i = l2.size()-1; i>0; i--) {
    			if(! l1.get(j).equals(l2.get(i))) {
    				ends = false;
    			}
    			j--;
    		}
    		return ends? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
    	}
    	else {
    		return Satisfiability.NOT_SATISFIED;
    	}
    }
    
    /*
     * left equals right?
     * Here we compare if every brick is equal respecting their order;
     */
    private Satisfiability stringEqualsAux(BricksDomain left, BricksDomain right) {
    	List<Brick> l1 = left.getBricks();
    	List<Brick> l2 = right.getBricks();
    	ListIterator<Brick> iter1 = l1.listIterator();
    	ListIterator<Brick> iter2 = l2.listIterator();
    	boolean flag = true;
    	while(iter1.hasNext() || iter2.hasNext()) {
    		if(! iter1.next().equals(iter2.next())) {
    			flag=false;
    		}
    	}
    	return flag?Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
    }
    
    /*
     * left starts with right?
     * We assume that l2 is shorter than l1
     */ 
    private Satisfiability stringStartsAux(BricksDomain left, BricksDomain right) {
    	List<Brick> l1 = left.getBricks();
    	List<Brick> l2 = right.getBricks();
    	boolean starts = true;
    	if(l2.size()<=l1.size()) {
    		for(int i = 0; i < l2.size(); i++) {
    			if(! l1.get(i).equals(l2.get(i))) {
    				starts = false;
    			}
    		}
    		return starts?Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
    	}else {
    		return Satisfiability.NOT_SATISFIED;
    	}
    }

    private BricksDomain stringReplace(BricksDomain input, BricksDomain search, BricksDomain replace) {
        List<Brick> l1 = input.getBricks();
        ArrayList<Brick> l2 = (ArrayList<Brick>) search.getBricks();
        List<Brick> output = new ArrayList<Brick>();
        for (int i = 0; i < l1.size(); i++) {
            if (l1.get(i) != l2.get(0)) {
                // if is different, add the i-th element of l1 to the output list.
                output.add(l1.get(i));
            } else {
                // they are the same. Check if it we need to replace.
                if (isPrefix(l1.subList(i, l1.size()), l2)) {
                    output.addAll(cloneOf(replace.getBricks()));
                    i += l2.size();
                } else {
                    // is not the prefix. Just skip.
                    output.add(l1.get(i));
                }
            }
        }
        return new BricksDomain(output);
    }

    private boolean isPrefix(List<Brick> l1, List<Brick> l2) {
        if (l1.size() != l2.size()) {
            return false;
        } else {
            for (int i = 0; i < l1.size(); i++) {
                if (l1.get(i) != l2.get(i)) {
                    return false;
                }
            }
            return true;
        }
    }

    private BricksDomain stringSubStr(BricksDomain input, int i, int j) {
        if (input.getBricks().size() < j || i < 0) {
            return new BricksDomain();
        }
        return new BricksDomain(input.getBricks().subList(i,j));
    }

    public static List<Brick> cloneOf(List<Brick> bricks) {
        List<Brick> cloned = new ArrayList<>();
        for (Brick b : bricks) {
            cloned.add(b.clone());
        }
        return cloned;
    }
}