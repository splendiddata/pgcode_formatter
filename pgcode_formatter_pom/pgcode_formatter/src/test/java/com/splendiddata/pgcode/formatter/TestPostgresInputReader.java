/*
 * Copyright (c) Splendid Data Product Development B.V. 2020
 *
 * This program is free software: You may redistribute and/or modify under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at Client's option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, Client should obtain one via www.gnu.org/licenses/.
 */

package com.splendiddata.pgcode.formatter;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;
import com.splendiddata.pgcode.formatter.scanner.structure.SrcNode;

/**
 * Some JUnit tests for the {@link PostgresInputReader}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestPostgresInputReader {

    /**
     * Tests some type casts in the form ::data-type
     *
     * @throws IOException
     *             No it doesn't
     */
    @Test
    public void typeCasts() throws IOException {
        String input = "::a :: /* with comment */ b :: -- comment\nc.d";
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(input))) {
            SrcNode result = PostgresInputReader.interpretStatementBody(reader.getFirstResult());
            Assertions.assertEquals("::a", result.toString(), "first type cast");
            result = PostgresInputReader.interpretStatementBody(result.getNext()); // whitespace
            result = PostgresInputReader.interpretStatementBody(result.getNext());
            Assertions.assertEquals(":: /* with comment */ b", result.toString(), "second type cast");
            result = PostgresInputReader.interpretStatementBody(result.getNext()); // whitespace
            result = PostgresInputReader.interpretStatementBody(result.getNext());
            Assertions.assertEquals(":: -- comment\n" + 
                    "c.d", result.toString(), "third type cast");
        }
    }

    /**
     * isolates all operators it can find the the testCase.getInput() and checks if they are the same as the expected
     * operators
     *
     * @param testCase
     *            the case to test
     * @throws IOException
     *             No, it doesn't
     */
    @ParameterizedTest
    @MethodSource("getOperatorTestCases")
    public void testOperators(@SuppressWarnings("exports") OperatorTestCase testCase) throws IOException {
        List<ScanResult> foundOperators = new ArrayList<>();
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(testCase.getInput()))) {
            for (SrcNode node = PostgresInputReader
                    .interpretStatementBody(reader.getFirstResult()); node != null; node = PostgresInputReader
                            .interpretStatementBody(node.getNext())) {
                if (node.is(ScanResultType.OPERATOR)) {
                    foundOperators.add(node);
                }
            }
            Assertions.assertIterableEquals(testCase.getExpectedOperators(),
                    foundOperators.stream().map(node -> node.toString()).collect(Collectors.toList()),
                    testCase.toString());

        }
    }

    /**
     * @return Stream&lt;OperatorTestCase&gt; The operator test cases
     */
    private static final Stream<OperatorTestCase> getOperatorTestCases() {
        Stream.Builder<OperatorTestCase> builder = Stream.builder();
        return builder.add(new OperatorTestCase("*", "*")).add(new OperatorTestCase("a+b", "+"))
                .add(new OperatorTestCase("a+-b", "+", "-")).add(new OperatorTestCase("a ==>- b", "==>", "-"))
                .add(new OperatorTestCase("a @==>- b", "@==>-"))
                .add(new OperatorTestCase("a +/-*+-+ b", "+/-*", "+", "-", "+"))
                .add(new OperatorTestCase("no operator at all.")).build();
    }

    /**
     * Test case for the operator test
     *
     * @author Splendid Data Product Development B.V.
     * @since 0.0.1
     */
    private static final class OperatorTestCase {
        private final String input;
        private final List<String> expectedOperators;

        /**
         * Constructor
         * 
         * @param input
         *            A string that is supposed to be some (plpg)sql input that contains some operators
         * @param expectedOperators
         *            Operators The operators that are supposed to be findable in the input in the order of appearance.
         */
        protected OperatorTestCase(String input, String... expectedOperators) {
            super();
            this.input = input;
            this.expectedOperators = Arrays.asList(expectedOperators);
        }

        /**
         * @return String the input that is to be interpreted
         */
        public String getInput() {
            return input;
        }

        /**
         * @return List&lt;String&gt; the operators that can be found in the input in order of appearance
         */
        public List<String> getExpectedOperators() {
            return expectedOperators;
        }

        /**
         * @see java.lang.Object#toString()
         *
         * @return String representing the input and the expectations
         */
        @Override
        public String toString() {
            return new StringBuilder().append("input = \"").append(input).append("\" should contain operators: \"")
                    .append(expectedOperators.stream().collect(Collectors.joining("\", \""))).append("\"").toString();
        }
    }
}
