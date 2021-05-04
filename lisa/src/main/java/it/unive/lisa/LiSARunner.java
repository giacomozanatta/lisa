package it.unive.lisa;

import static it.unive.lisa.LiSAFactory.getInstance;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.types.InferredTypes;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.checks.ChecksExecutor;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.file.FileManager;

public class LiSARunner<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> {

	private static final Logger log = LogManager.getLogger(LiSARunner.class);

	private final LiSAConfiguration conf;

	private final InterproceduralAnalysis<A, H, V> interproc;

	private final CallGraph callGraph;

	private final A state;

	LiSARunner(LiSAConfiguration conf, InterproceduralAnalysis<A, H, V> interproc, CallGraph callGraph,
			A state) {
		this.conf = conf;
		this.interproc = interproc;
		this.callGraph = callGraph;
		this.state = state;
	}

	Collection<Warning> run(Program program, FileManager fileManager)
			throws AnalysisExecutionException {
		finalizeProgram(program);

		Collection<CFG> allCFGs = program.getAllCFGs();

		if (conf.isDumpCFGs())
			for (CFG cfg : IterationLogger.iterate(log, allCFGs, "Dumping input CFGs", "cfgs"))
				dumpCFG(fileManager, "", cfg, st -> "");

		CheckTool tool = new CheckTool();
		if (!conf.getSyntacticChecks().isEmpty())
			ChecksExecutor.executeAll(tool, program, conf.getSyntacticChecks());
		else
			log.warn("Skipping syntactic checks execution since none have been provided");

		try {
			callGraph.build(program);
		} catch (CallGraphConstructionException e) {
			log.fatal("Exception while building the call graph for the input program", e);
			throw new AnalysisExecutionException("Exception while building the call graph for the input program", e);
		}

		try {
			interproc.build(program, callGraph);
		} catch (InterproceduralAnalysisException e) {
			log.fatal("Exception while building the interprocedural analysis for the input program", e);
			throw new AnalysisExecutionException(
					"Exception while building the interprocedural analysis for the input program", e);
		}

		if (conf.isInferTypes())
			inferTypes(fileManager, program, allCFGs);
		else
			log.warn("Type inference disabled: dynamic type information will not be available for following analysis");

		if (state != null) {
			analyze(allCFGs, fileManager);
			Map<CFG, Collection<CFGWithAnalysisResults<A, H, V>>> results = new IdentityHashMap<>(allCFGs.size());
			for (CFG cfg : allCFGs)
				results.put(cfg, interproc.getAnalysisResultsOf(cfg));

			if (!conf.getSemanticChecks().isEmpty()) {
				CheckToolWithAnalysisResults<A, H,
						V> toolWithResults = new CheckToolWithAnalysisResults<>(tool, results);
				tool = toolWithResults;
				ChecksExecutor.executeAll(toolWithResults, program, conf.getSemanticChecks());
			} else
				log.warn("Skipping semantic checks execution since none have been provided");
		} else
			log.warn("Skipping analysis execution since no abstract sate has been provided");

		return tool.getWarnings();
	}

	private void analyze(Collection<CFG> allCFGs, FileManager fileManager) {
		A state = this.state.top();
		TimerLogger.execAction(log, "Computing fixpoint over the whole program",
				() -> {
					try {
						interproc.fixpoint(new AnalysisState<>(state, new Skip()));
					} catch (FixpointException e) {
						log.fatal("Exception during fixpoint computation", e);
						throw new AnalysisExecutionException("Exception during fixpoint computation", e);
					}
				});

		if (conf.isDumpAnalysis())
			for (CFG cfg : IterationLogger.iterate(log, allCFGs, "Dumping analysis results", "cfgs")) {
				for (CFGWithAnalysisResults<A, H, V> result : interproc.getAnalysisResultsOf(cfg))
					dumpCFG(fileManager, "analysis___" + (result.getId() == null ? "" : result.getId().hashCode() + "_"), result,
							st -> result.getAnalysisStateAfter(st).toString());
			}
	}

	@SuppressWarnings("unchecked")
	private void inferTypes(FileManager fileManager, Program program, Collection<CFG> allCFGs) {
		SimpleAbstractState<H, InferenceSystem<InferredTypes>> typesState;
		InterproceduralAnalysis<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
				InferenceSystem<InferredTypes>> typesInterproc;
		try {
			// type inference is executed with the simplest abstract state
			InferenceSystem<InferredTypes> types = new InferenceSystem<>(new InferredTypes());
			HeapDomain<?> heap = state == null ? LiSAFactory.getDefaultFor(HeapDomain.class) : state.getHeapState();
			typesState = getInstance(SimpleAbstractState.class, heap, types).top();
			typesInterproc = getInstance(interproc.getClass());
			typesInterproc.build(program, callGraph);
		} catch (AnalysisSetupException | InterproceduralAnalysisException e) {
			throw new AnalysisExecutionException("Unable to initialize type inference", e);
		}

		TimerLogger.execAction(log, "Computing type information",
				() -> {
					try {
						typesInterproc.fixpoint(new AnalysisState<>(typesState, new Skip()));
					} catch (FixpointException e) {
						log.fatal("Exception during fixpoint computation", e);
						throw new AnalysisExecutionException("Exception during fixpoint computation", e);
					}
				});

		String message = conf.isDumpTypeInference()
				? "Dumping type analysis and propagating it to cfgs"
				: "Propagating type information to cfgs";
		for (CFG cfg : IterationLogger.iterate(log, allCFGs, message, "cfgs")) {
			Collection<CFGWithAnalysisResults<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
					InferenceSystem<InferredTypes>>> results = typesInterproc.getAnalysisResultsOf(cfg);
			if (results.isEmpty())
				log.warn("No type information computed for '" + cfg + "': it is unreachable");

			CFGWithAnalysisResults<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
					InferenceSystem<InferredTypes>> result = null;
			try {
				for (CFGWithAnalysisResults<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
						InferenceSystem<InferredTypes>> res : results)
					if (result == null)
						result = res;
					else
						result = result.lub(cfg, res);
			} catch (SemanticException e) {
				throw new AnalysisExecutionException("Unable to compute type information for " + cfg, e);
			}

			cfg.accept(new TypesPropagator<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H>(), result);

			CFGWithAnalysisResults<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
					InferenceSystem<InferredTypes>> r = result;
			if (conf.isDumpTypeInference())
				dumpCFG(fileManager, "typing___", r, st -> r.getAnalysisStateAfter(st).toString());
		}
	}

	private static class TypesPropagator<A extends AbstractState<A, H, InferenceSystem<InferredTypes>>,
			H extends HeapDomain<H>>
			implements
			GraphVisitor<CFG, Statement, Edge, CFGWithAnalysisResults<A, H, InferenceSystem<InferredTypes>>> {

		@Override
		public boolean visit(CFGWithAnalysisResults<A, H, InferenceSystem<InferredTypes>> tool, CFG graph) {
			return true;
		}

		@Override
		public boolean visit(CFGWithAnalysisResults<A, H, InferenceSystem<InferredTypes>> tool, CFG graph, Edge edge) {
			return true;
		}

		@Override
		public boolean visit(CFGWithAnalysisResults<A, H, InferenceSystem<InferredTypes>> tool, CFG graph,
				Statement node) {
			if (tool != null && node instanceof Expression)
				((Expression) node).setRuntimeTypes(tool.getAnalysisStateAfter(node).getState().getValueState()
						.getInferredValue().getRuntimeTypes());
			return true;
		}
	}

	private void finalizeProgram(Program program) {
		// fill up the types cache by side effect on an external set
		Caches.types().clear();
		ExternalSet<Type> types = Caches.types().mkEmptySet();
		program.getRegisteredTypes().forEach(types::add);
		types = null;

		TimerLogger.execAction(log, "Finalizing input program", () -> {
			try {
				program.validateAndFinalize();
			} catch (ProgramValidationException e) {
				throw new AnalysisExecutionException("Unable to finalize target program", e);
			}
		});
	}

	private void dumpCFG(FileManager fileManager, String filePrefix, CFG cfg,
			Function<Statement, String> labelGenerator) {
		try (Writer file = fileManager.mkDotFile(filePrefix + cfg.getDescriptor().getFullSignatureWithParNames())) {
			cfg.dump(file, st -> labelGenerator.apply(st));
		} catch (IOException e) {
			log.error("Exception while dumping the analysis results on " + cfg.getDescriptor().getFullSignature(),
					e);
		}
	}
}
