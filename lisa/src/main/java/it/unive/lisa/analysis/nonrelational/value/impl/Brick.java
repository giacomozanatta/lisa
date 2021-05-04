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

}
