package it.unive.lisa.bricks;

import it.unive.lisa.LiSA;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.BricksDomain;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import org.junit.Test;

import it.unive.lisa.AnalysisException;


public class BricksDomainTest {

    @Test
    public void testBricks() throws ParsingException, AnalysisException {
        new LiSA(
                new LiSAConfiguration().setDumpAnalysis(true)
                .setWorkdir("test-outputs/sign-domain")
                .setAbstractState(LiSAFactory.getDefaultFor(AbstractState.class,
                                LiSAFactory.getDefaultFor(HeapDomain.class), new BricksDomain(null)))
        ).run(IMPFrontend.processFile("imp-testcases/sign/program.imp"));
    }
}