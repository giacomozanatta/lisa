package it.unive.lisa.analysis.nonrelational.value.impl;

import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
public class Brick {


	Set<String> strings;
	Optional<Integer> min;
	Optional<Integer> max;
	
	public Brick(Set<String> strings, int min, int max) {
		this.strings = strings;
		this.min = Optional.of(min);
		this.max = Optional.of(max);
	}
	
	/*
	 * Specific constructor for max infinity value
	 */
	public Brick(Set<String> strings, int min) {
		this.strings = strings;
		this.min = Optional.of(min);
		this.max = Optional.empty();
	}

	public Brick() {
		this.strings = new TreeSet<>();
		this.min = Optional.of(0);
		this.max = Optional.of(0);
	}
	
	@Override
	public String toString() {
		return strings + " min=" + getMin() + " max=" + getMax();
	}
	
	public Set<String> getStrings() {
		return strings;
	}
	
	public int getMin() {
		return min.get();
	}
	
	public int getMax() {
		if(max.isPresent()) {
			return max.get();
		}
		else {
			return Integer.MAX_VALUE;
		}
	}

	public void setStrings(Set<String> strings) {
		this.strings = strings;
	}

	public void setMin(int min) {
		this.min = Optional.of(min);
	}

	public void setMax(int max) {
		this.max = Optional.of(max);
	}

	public static List<Brick> normalize(List<Brick> brList) {
		if(brList.size()==1 && brList.get(0) instanceof TopBrick)
			return brList;
		List<Brick> cloned = BricksDomain.cloneOf(brList);
		rule1(cloned);
		rule2(cloned);
		rule3(cloned);
		rule4(cloned);
		rule5(cloned);
		return cloned;
	}


	private static void rule1(List<Brick> brList) {
		brList.removeIf(br -> 
				!(br instanceof TopBrick) &&
				br.getStrings().isEmpty() &&
				br.getMax() == 0 &&
				br.getMin() == 0);
	}

	private static void rule2(List<Brick> brList) {
		ListIterator<Brick> iter = brList.listIterator();
		while( iter.hasNext()) {
			
			//System.out.println("Loop 2");
			Brick first = iter.next();

			if( first.getMin() == 1 &&
					first.getMax() == 1 &&
					iter.hasNext() )
			{
				Brick second = iter.next();
				if (second.getMin() == 1 &&
						second.getMax() == 1)
				{

					first.setStrings(stringSetConcatenation(first.getStrings(), second.getStrings()));

					// remove second brick
					iter.remove();
				}
			}
		}
	}

	private static void rule3(List<Brick> brList) {
		ListIterator<Brick> iter = brList.listIterator();
		while (iter.hasNext()) {
			//System.out.println("Loop 3");
			Brick br = iter.next();
			if(br.getMin() == br.getMax() && br.getMin()!=1) {
				// TODO concatenation of all strings n times
				br.setStrings(stringsConcatenation(br.getStrings(), br.getMax()));
			}
		}
	}

	private static void rule4(List<Brick> brList) {
		ListIterator<Brick> iter = brList.listIterator();
		while( iter.hasNext()) {
			//System.out.println("Loop 4");
			Brick first = iter.next();

			if( iter.hasNext()){
				Brick second = iter.next();
				if (first.getStrings().equals(second.getStrings())) {
					// modify indices
					first.setMin(first.getMin() + second.getMin());
					first.setMax(first.getMax() + second.getMax());
					// remove second brick
					iter.remove();
				}
				else {
					iter.previous();
				}
			}
		}
	}

	private static void rule5(List<Brick> brList) {
		ListIterator<Brick> iter = brList.listIterator();
		List<Brick> toBeAdded = new ArrayList<Brick>();
		while (iter.hasNext()) {
			//System.out.println("Loop 5");
			Brick br = iter.next();
			if(br.getMin()>0 && br.getMax() != br.getMin()) {
				// creation of the first brick
				// TODO concatenation of all strings n times
				Brick newBrick1 = new Brick(stringsConcatenation(br.getStrings(), br.getMin()), 1, 1);
				Brick newBrick2 = new Brick(br.getStrings(), 0, br.getMax() - br.getMin());
				toBeAdded.add(newBrick1);
				toBeAdded.add(newBrick2);
				iter.remove();
			}
			else {
				toBeAdded.add(br);
				iter.remove();
			}
		}
		brList.addAll(toBeAdded);
	}
	
	private static Set<String> stringSetConcatenation(Set<String> firstSet, Set<String> secondSet){
		Set<String> newSet = new TreeSet<String>();
		for(String s1 : firstSet) {
			for(String s2 : secondSet) {
				newSet.add(s1.concat(s2));
			}
		}
		return newSet;
	}

	private static Set<String> stringsConcatenation(Set<String> strings, int nTimes){
		Set<String> newSet = new TreeSet<String>();
		for(String s1 : strings) {
			Set<String> tmpSet1 = new TreeSet<String>();
			Set<String> tmpSet2 = new TreeSet<String>();
			tmpSet1.addAll(strings);
			for(int i = 0; i < nTimes-1; i++) {
				for(String s2 : tmpSet1) {
				    // System.out.println(s2);
					tmpSet2.add(s1.concat(s2));
				}
				tmpSet1.clear();
				tmpSet1.addAll(tmpSet2);
				tmpSet2.clear();
			}
			newSet.addAll(tmpSet1);
			
		}
		return newSet;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + max.get();
		result = prime * result + min.get();
		result = prime * result + ((strings == null) ? 0 : strings.hashCode());
		return result;
	}

	public Brick clone() {
		if(this instanceof TopBrick)
			return this;
		Set<String> strings = new TreeSet<String>();
		strings.addAll(this.strings);
		return new Brick(strings, this.getMin(), this.getMax());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Brick other = (Brick) obj;
		if (max != other.max)
			return false;
		if (min != other.min)
			return false;
		if (strings == null) {
			if (other.strings != null)
				return false;
		} else if (!strings.equals(other.strings))
			return false;
		return true;
	}

	public int compareTo(Brick brick2) {
		if ((brick2.strings.containsAll(strings) && this.getMin() >= brick2.getMin() && getMax() <= brick2.getMax()) ||
				(brick2 instanceof TopBrick) || (this instanceof BottomBrick)) {
			return -1; // brick1 is smaller
		}
		return 1; // brick2 is smaller
	}
	
	public boolean padCompare(Brick brick2) {
		if( this.getMin() == brick2.getMin() &&
			this.getMax() == brick2.getMax() &&
			//this.strings.size() == brick2.strings.size() &&
			this.strings.equals(brick2.strings) )
			return true;
		else
			return false;
	}

	// return the lub between two bricks
	public Brick lub(Brick brick2) {
		if(this instanceof TopBrick || brick2 instanceof TopBrick)
			return new TopBrick();
		Set<String> union = new TreeSet<>();
		union.addAll(strings);
		union.addAll(brick2.strings);
		return new Brick(union, Math.min(getMin(), brick2.getMin()), Math.max(getMax(), brick2.getMax()));
	}
}