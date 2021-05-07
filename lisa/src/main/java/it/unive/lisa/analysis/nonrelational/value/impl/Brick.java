package it.unive.lisa.analysis.nonrelational.value.impl;

import java.util.Set;

public class Brick {

	Set<String> strings;
	int min;
	int max;
	public Brick(Set<String> strings, int min, int max) {
		this.strings = strings;
		this.min = min;
		this.max = max;
	}
	public Brick() {} // represents empty brick
	public Set<String> getStrings() {
		return strings;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
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
		if ((brick2.strings.containsAll(strings) && min > brick2.min && max < brick2.max) ||
				(brick2 instanceof TopBrick) || (this instanceof BottomBrick)) {
			return -1; // brick1 is smaller
		}
		return 1; // brick2 is smaller
	}
}
