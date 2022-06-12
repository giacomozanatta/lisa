package it.unive.lisa.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.type.ArrayType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An expression modeling the array element access operation
 * ({@code array[index]}). The type of this expression is the one of the
 * resolved array element.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPArrayAccess extends BinaryExpression {

	/**
	 * Builds the array access.
	 * 
	 * @param cfg        the {@link ImplementedCFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param container  the expression representing the array reference that
	 *                       will receive the access
	 * @param location   the expression representing the accessed element
	 */
	public IMPArrayAccess(ImplementedCFG cfg, String sourceFile, int line, int col, Expression container,
			Expression location) {
		super(cfg, new SourceCodeLocation(sourceFile, line, col), "[]", container, location);
	}

	@Override
	protected <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> binarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		ExternalSet<Type> arraytypes = Caches.types().mkEmptySet();
		for (Type t : left.getRuntimeTypes())
			if (t.isPointerType() && t.asPointerType().getInnerTypes().anyMatch(Type::isArrayType))
				arraytypes.addAll(t.asPointerType().getInnerTypes().filter(Type::isArrayType));

		if (arraytypes.isEmpty())
			return state.bottom();

		ArrayType arraytype = arraytypes.reduce(arraytypes.first(), (r, t) -> r.commonSupertype(t)).asArrayType();
		HeapDereference container = new HeapDereference(arraytype, left, getLocation());
		container.setRuntimeTypes(arraytypes);

		return state.smallStepSemantics(new AccessChild(arraytype.getInnerType(), container, right, getLocation()),
				this);
	}
}
