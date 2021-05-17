package it.unive.lisa.analysis.nonrelational.value.impl;

import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
public class Brick {

	Set<String> strings;
	int min;
	int max;
	
	public Brick(Set<String> strings, int min, int max) {
		this.strings = strings;
		this.min = min;
		this.max = max;
	}

	public Brick() {}
	public Set<String> getStrings() {
		return strings;
	}
	
	public int getMin() {
		return min;
	}
	
	public int getMax() {
		return max;
	}

	public void setStrings(Set<String> strings) {
		this.strings = strings;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public List<Brick> normalize(List<Brick> brList) {
		this.rule1(brList);
		this.rule2(brList);
		this.rule3(brList);
		this.rule4(brList);
		this.rule5(brList);
		return brList;
	}


	private void rule1(List<Brick> brList) {
		brList.removeIf(br -> br.getStrings().isEmpty() &&
				br.getMax() == 0 &&
				br.getMin() == 0);
	}

	private void rule2(List<Brick> brList) {
		ListIterator<Brick> iter = brList.listIterator();
		while( iter.hasNext()) {
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

	private void rule3(List<Brick> brList) {
		ListIterator<Brick> iter = brList.listIterator();
		while (iter.hasNext()) {
			Brick br = iter.next();
			if(br.getMin() == br.getMax()) {
				// TODO concatenation of all strings n times
				this.stringsConcatenation(br.getStrings(), 2);
				br.setStrings(null);
			}
		}
	}

	private void rule4(List<Brick> brList) {
		ListIterator<Brick> iter = brList.listIterator();
		while( iter.hasNext()) {
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
			}
		}
	}

	private void rule5(List<Brick> brList) {
		ListIterator<Brick> iter = brList.listIterator();
		List<Brick> toBeAdded = new ArrayList<Brick>();
		while (iter.hasNext()) {
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
		}
		brList.addAll(toBeAdded);
	}
	
	private Set<String> stringSetConcatenation(Set<String> firstSet, Set<String> secondSet){
		Set<String> newSet = new TreeSet<String>();
		for(String s1 : firstSet) {
			for(String s2 : secondSet) {
				newSet.add(s1.concat(s2));
			}
		}
		return newSet;
	}

	private Set<String> stringsConcatenation(Set<String> strings, int nTimes){
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
		result = prime * result + max;
		result = prime * result + min;
		result = prime * result + ((strings == null) ? 0 : strings.hashCode());
		return result;
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
		if ((brick2.strings.containsAll(strings) && min >= brick2.min && max <= brick2.max) ||
				(brick2 instanceof TopBrick) || (this instanceof BottomBrick)) {
			return -1; // brick1 is smaller
		}
		return 1; // brick2 is smaller
	}

	// return the lub between two bricks
	public Brick lub(Brick brick2) {
		Set<String> union = new TreeSet<>(strings);
		union.addAll(brick2.strings);
		return new Brick(union, Math.min(min, brick2.min), Math.min(max, brick2.max));
	}
}