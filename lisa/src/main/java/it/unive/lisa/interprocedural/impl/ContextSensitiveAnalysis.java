package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;

public class ContextSensitiveAnalysis<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> extends ContextBasedAnalysis<A, H, V> {

	public ContextSensitiveAnalysis() {
		super(CallPointContextSensitiveToken.getSingleton());
	}
}
