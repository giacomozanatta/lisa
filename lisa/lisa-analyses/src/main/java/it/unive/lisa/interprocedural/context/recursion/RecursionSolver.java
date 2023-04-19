package it.unive.lisa.interprocedural.context.recursion;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.ContextSensitivityToken;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A recursion solver that applies the iterates of the recursion starting from
 * bottom. This solver operates by restarting the recursion from
 * {@link Recursion#getInvocation()} a number of times, until the results of all
 * the members stabilize.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *                abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *                abstract state
 * @param <T> the type of {@link TypeDomain} contained into the computed
 *                abstract state
 */
public class RecursionSolver<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends ContextBasedAnalysis<A, H, V, T> {

	private static final Logger LOG = LogManager.getLogger(RecursionSolver.class);

	private final Recursion<A, H, V, T> recursion;

	private final boolean returnsVoid;

	private final Map<CFGCall, Pair<AnalysisState<A, H, V, T>, ContextSensitivityToken>> finalEntryStates;

	private GenericMapLattice<CFGCall, AnalysisState<A, H, V, T>> previousApprox;

	private GenericMapLattice<CFGCall, AnalysisState<A, H, V, T>> recursiveApprox;

	private ContextSensitivityToken headToken;

	/**
	 * Builds the solver.
	 * 
	 * @param backing   the analysis that backs this solver, and that can be
	 *                      used to query call results
	 * @param recursion the recursion to solve
	 */
	public RecursionSolver(ContextBasedAnalysis<A, H, V, T> backing, Recursion<A, H, V, T> recursion) {
		super(backing);
		this.recursion = recursion;
		finalEntryStates = new HashMap<>();

		// the return value of each back call must be the same as the one
		// starting the recursion, as they invoke the same cfg
		returnsVoid = returnsVoid(recursion.getInvocation(), null);
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy)
			throws InterproceduralAnalysisException {
		// we mark this as unsupported to make sure it never gets used as a root
		// analysis
		throw new UnsupportedOperationException();
	}

	@Override
	public void fixpoint(
			AnalysisState<A, H, V, T> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			FixpointConfiguration conf)
			throws FixpointException {
		// we mark this as unsupported to make sure it never gets used as a root
		// analysis
		throw new UnsupportedOperationException();
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		boolean inRecursion = recursion.getMembers().contains(call.getCFG());
		if (headToken == null && inRecursion)
			// this is the first time we handle a call from within the recursion
			headToken = token;

		if (inRecursion && call.getTargetedCFGs().contains(recursion.getRecursionHead())) {
			// this is a back call
			finalEntryStates.put(call, Pair.of(entryState, token));

			AnalysisState<A, H, V, T> approx = null;
			if (recursiveApprox.getMap() != null)
				approx = recursiveApprox.getMap().get(call);
			if (approx == null)
				// no state: we must start at bottom
				if (returnsVoid)
					return entryState.bottom();
				else
					return new AnalysisState<>(
							entryState.getState().bottom(),
							call.getMetaVariable(),
							entryState.getAliasing().bottom());
			else
				// we already have a state to use
				// we bring in the entry state to carry over the correct scope
				return approx.lub(entryState);
		}
		return super.getAbstractResultOf(call, entryState, parameters, expressions);
	}

	@Override
	protected boolean canShortcut(CFG cfg) {
		// we want to compute the recursive chain with no shortcuts
		return !recursion.getMembers().contains(cfg);
	}

	@Override
	protected boolean shouldCheckForRecursions() {
		return false;
	}

	/**
	 * Solves the recursion by applying its iterates starting from bottom.
	 * 
	 * @throws SemanticException if an exception happens during the computation
	 */
	public void solve() throws SemanticException {
		int recursionCount = 0;
		Call start = recursion.getInvocation();
		Collection<CFGCall> ends = finalEntryStates.keySet();
		CompoundState<A, H, V, T> entryState = recursion.getEntryState();

		LOG.info("Solving recursion at " + start.getLocation() + " for context " + recursion.getInvocationToken());

		recursiveApprox = new GenericMapLattice<>(entryState.postState.bottom());
		recursiveApprox = recursiveApprox.bottom();

		do {
			LOG.debug(StringUtilities.ordinal(recursionCount + 1)
					+ " evaluation of recursive chain at "
					+ start.getLocation());

			previousApprox = recursiveApprox;

			// we reset the analysis at the point where the starting call can be
			// evaluated
			token = recursion.getInvocationToken();
			AnalysisState<A, H, V, T> post = start.semantics(entryState.postState, this, entryState.intermediateStates);

			for (CFGCall end : ends) {
				AnalysisState<A, H, V, T> res = post.bottom();
				Identifier meta = end.getMetaVariable();
				if (returnsVoid)
					res = post;
				else
					for (Identifier variable : start.getMetaVariables())
						// we transfer the return value
						res = res.lub(post.assign(meta, variable, start));

				// we only keep variables that can be affected by the recursive
				// chain: the whole recursion return value, and all variables
				// that are not sensible to scoping. All other variables are
				// subjected to push and pop operations and cannot be
				// considered a contribution of the recursive call.
				res = res.forgetIdentifiersIf(i -> i.canBeScoped() && !i.equals(meta));
				recursiveApprox = recursiveApprox.putState(end, res);
			}

			if (conf.wideningThreshold < 0)
				recursiveApprox = previousApprox.lub(recursiveApprox);
			else if (recursionCount == conf.wideningThreshold)
				recursiveApprox = previousApprox.widening(recursiveApprox);
			else {
				recursionCount++;
				recursiveApprox = previousApprox.lub(recursiveApprox);
			}
		} while (!recursiveApprox.lessOrEqual(previousApprox));

		if (conf.optimize)
			// as the fixpoint results do not contain an explicit entry for the
			// recursive call, we need to store the approximation for the
			// recursive call manually or the unwinding won't manage to solve it
			for (CFGCall call : ends) {
				Pair<AnalysisState<A, H, V, T>, ContextSensitivityToken> pair = finalEntryStates.get(call);
				AnalysisState<A, H, V, T> callEntry = pair.getLeft();
				ContextSensitivityToken callingToken = pair.getRight();

				// we get the cfg containing the call
				OptimizedAnalyzedCFG<A, H, V, T> caller = (OptimizedAnalyzedCFG<A, H, V, T>) results.get(call.getCFG())
						.get(callingToken);

				// we get the actual call that is part of the cfg
				Call source = call;
				if (call.getSource() != null)
					source = call.getSource();

				// it might happen that the call is a hotspot and we don't need
				// any additional work
				if (!caller.hasPostStateOf(source)) {
					// we take the value returned to the start of the recursion
					AnalysisState<A, H, V, T> exit = results.get(recursion.getRecursionHead())
							.get(headToken)
							.getExitState();

					// from that, we only keep the value being returned
					AnalysisState<A, H, V, T> polished = exit.forgetIdentifiersIf(
							i -> !exit.getComputedExpressions().contains(i));

					// we add the value to the entry state
					AnalysisState<A, H, V, T> returned = callEntry.lub(polished);

					// we assign the value to the call's meta variable, and we
					// then forget about it
					AnalysisState<A, H, V, T> post = returned.bottom();
					Identifier meta = call.getMetaVariable();
					Collection<Identifier> toRemove = new HashSet<>();
					for (SymbolicExpression r : returned.getComputedExpressions()) {
						post = post.lub(returned.assign(meta, r, call));
						if (r instanceof Identifier)
							toRemove.add((Identifier) r);
					}
					post = post.forgetIdentifiers(toRemove);

					// finally, we store it in the result
					caller.storePostStateOf(source, post);
				}
			}
	}
}
