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

package com.splendiddata.pgcode.formatter.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

import com.splendiddata.pgcode.formatter.scanner.FormattedSourceScannerImpl;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.SourceScanner;

/**
 * Is the starting point for scanning the source code and contains utility methods to interpret it.
 * <p>
 * The first ScanResult can be obtained via the {@link #getFirstResult()} method directly after construction. Subsequent
 * ScanResults are returned by the {@link ScanResult#getNext()} method.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class FormattedInputReader implements Closeable {

    private static SourceScanner scanner;
    private ScanResult scanResult;

    /**
     * Constructor
     * <p>
     * The constructor starts interpreting the first node(s) of the input that it gets from the reader. Using the
     * {@link #getFirstResult()} method, the first result will be made available. Following results can be obtained via
     * the {@link ScanResult#getNext()} method on the returned result. The next result may come directly from the
     * scanner, which will invoke the reader if necessary, or it may have been read before. Thus the source can be
     * parsed in a streaming way.
     *
     * @param reader
     *            A Reader that will provide the input.
     * @throws IOException
     *             from the reader
     */
    public FormattedInputReader(Reader reader) throws IOException {
        scanner = new FormattedSourceScannerImpl(reader);
        scanResult = scanner.scan();
    }

    /**
     * Returns the first ScanResult from the input.
     * <p>
     * BEWARE! The first result can be obtained only ONCE. Following ScanResults can be obtained via the ScanResult's
     * getNext() method. This is to accommodate read ahead techniques to properly interpret the code. For example, to
     * interpret a statement that creates a function, the nodes CREATE - space - OR - space - REPLACE - space - FUNCTION
     * may need to be read while the statement will start at CREATE.
     *
     * @return ScanResult if invoked directly as first statement after the constructor or null in all other cases.
     */
    public ScanResult getFirstResult() {
        ScanResult result = scanResult;
        scanResult = null;
        return result;
    }

    @Override
    public void close() throws IOException {
        scanner.yyclose();
    }
}
