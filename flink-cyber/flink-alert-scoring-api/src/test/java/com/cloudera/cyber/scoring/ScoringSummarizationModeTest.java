package com.cloudera.cyber.scoring;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ScoringSummarizationModeTest {

    @Test
    public void testDefault(){
        assertThat(ScoringSummarizationMode.DEFAULT(), equalTo(ScoringSummarizationMode.POSITIVE_MEAN));
    }

    @Test
    public void testSum(){
        List<Double> scoreList = Arrays.asList(-1.0, 1.0, 2.0, 6.0);
        final ScoringSummarizationMode summarizationMode = ScoringSummarizationMode.SUM;

        verifyScore(summarizationMode, scoreList, 8);
    }

    @Test
    public void testMax(){
        List<Double> scoreList = Arrays.asList(-1.0, 1.0, 2.0, 6.0);
        final ScoringSummarizationMode summarizationMode = ScoringSummarizationMode.MAX;

        verifyScore(summarizationMode, scoreList, 6);
    }

    @Test
    public void testMean(){
        List<Double> scoreList = Arrays.asList(-1.0, 1.0, 2.0, 6.0);
        final ScoringSummarizationMode summarizationMode = ScoringSummarizationMode.MEAN;

        verifyScore(summarizationMode, scoreList, 2);
    }

    @Test
    public void testPositiveMean(){
        List<Double> scoreList = Arrays.asList(-1.0, 1.0, 2.0, 6.0);
        final ScoringSummarizationMode summarizationMode = ScoringSummarizationMode.POSITIVE_MEAN;

        verifyScore(summarizationMode, scoreList, 3);
    }

    private void verifyScore(ScoringSummarizationMode summarizationMode, List<Double> scoreList, double expectedScore) {
        final Double score = summarizationMode.calculateScore(scoreList);
        assertThat("Cyber score isn't expected!", score, equalTo(expectedScore));
    }

}