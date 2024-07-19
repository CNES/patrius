/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.optim.joptimizer.util.ArrayUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class for reading double precision records from DAFs readContentDPRecord, getContentSummaryRecord are the only
 * approved means for reading double precision records to and from DAFs.<br>
 * They keep track of which records have been read most recently, and of which records have been requested most often,
 * in order to minimize the amount of time spent actually reading from external storage.
 * 
 * <p>
 * This class is based on the dafwrd.for file of the SPICE library
 * </p>
 * 
 * @author T0281925
 *
 * @since 4.11
 */
public final class ReadDoublePrecisionDAF {

    /** Reading access identifier. */
    public static final String READ_ACCESS = "r";

    /** Number of bytes that represent a double precision number. */
    public static final int BYTES_DOUBLE = 8;

    /** Size of the record buffer. */
    private static final int RECORD_BUFFER_SIZE = 100;

    /** Number of doubles in a record. */
    private static final int NDOUBLES = 128;

    /** List containing the handle of the records read. */
    private static final int[] RBHAN = new int[RECORD_BUFFER_SIZE];

    /** List containing the record number of the records read. */
    private static final int[] RBREC = new int[RECORD_BUFFER_SIZE];

    /** List containing the times each record has been requested of the records read. */
    private static final int[] RBREQ = new int[RECORD_BUFFER_SIZE];

    /** Table containing the double precision data of each one of the records read. */
    private static final double[][] RBDAT = new double[NDOUBLES][RECORD_BUFFER_SIZE];

    /** Number of elements stored in the lists. */
    private static int rbnbr = 0; // Max index filled in

    /** Number of reads done. */
    private static int nRead = 0;

    /** Request counter. */
    private static int nReq = 0;

    /**
     * Constructor.
     */
    private ReadDoublePrecisionDAF() {
        // Nothing to do
    }

    /**
     * Read a portion of the contents of a summary record in a DAF file.
     * <p>
     * Checks the DAF record buffer to see if the requested record can be returned without actually reading it from
     * external storage. If not, it reads the record and stores it in the buffer, typically removing another record from
     * the buffer as a result.
     * </p>
     * <p>
     * Bad values of begin and end are not signaled as errors. The for loop will manage.
     * </p>
     * <p>
     * Based on the DAFGSR routine from the SPICE library.
     * </p>
     * 
     * @param handle
     *        the handle associated with a DAF
     * @param record
     *        the record number of a particular double precision record within the DAF, whose contents are to be read
     * @param begin
     *        the first word in the specified record to be returned
     * @param end
     *        the final word in the specified record to be returned
     * @param found
     *        (out) true if the specified record was found
     * @return contains the specified portion of the specified record from the specified file
     * @throws PatriusException
     *         if there is a problem in the reading of the file
     */
    public static double[] getContentSummaryRecord(final int handle, final int record, final int begin, final int end,
                                                   final boolean[] found) throws PatriusException {
        // Assume that the record will be found until proven otherwise.
        found[0] = true;
        double[] data = new double[NDOUBLES];

        // First, find the record.

        // If the specified handle and record number match those of a buffered record, determine the location of that
        // record within the buffer
        int bufloc = -1;
        boolean done = false;
        boolean stored = false;

        while (!done) {
            bufloc++;
            stored = (handle == RBHAN[bufloc] && record == RBREC[bufloc]);
            done = (stored || bufloc == rbnbr);
        }

        // If not, determine the location of the least recently requested record (the one with the smallest request
        // number). Get the unit number for the file, and read the record into this location.

        // If an error occurs while reading the record, clear the entire buffer entry in case the entry was corrupted by
        // a partial read.
        // Otherwise, increment the number of reads performed so far.
        if (!stored) {
            bufloc = ArrayUtils.getArrayMinIndex(RBREQ);

            final int[] ndni = DafHandle.getSummaryFormatDAF(handle);

            final boolean[] fnd = new boolean[1];
            try {
                data = readSumDescRecord(handle, record, ndni[0], ndni[1], fnd);
                for (int i = 0; i < NDOUBLES; i++) {
                    RBDAT[i][bufloc] = data[i];
                    data[i] = 0;
                }
            } catch (final IOException e) {
                found[0] = false;
                RBHAN[bufloc] = 0;
                RBREC[bufloc] = 0;
                RBREQ[bufloc] = 0;
            }

            if (!fnd[0]) {
                RBHAN[bufloc] = 0;
                RBREC[bufloc] = 0;
                RBREQ[bufloc] = 0;
            } else {
                if (nRead < Integer.MAX_VALUE) {
                    nRead++;
                }

                RBHAN[bufloc] = handle;
                RBREC[bufloc] = record;

                if (rbnbr < RECORD_BUFFER_SIZE - 1) {
                    rbnbr++;
                }
            }
        }

        // Whether previously stored or just read, the record is now in the buffer. Return the specified portion
        // directly, and increment the corresponding request number.
        if (found[0]) {
            getResultAndIncrementRequest(data, begin, end, bufloc);
        }

        return data;
    }

    /**
     * Read a portion of the contents of a double precision record in a DAF file.
     * <p>
     * Checks the DAF record buffer to see if the requested record can be returned without actually reading it from
     * external storage. If not, it reads the record and stores it in the buffer, typically removing another record from
     * the buffer as a result.
     * </p>
     * <p>
     * Bad values of begin and end are not signaled as errors. The for loop will manage.
     * </p>
     * <p>
     * Based on the DAFGDR routine from the SPICE library.
     * </p>
     * 
     * @param handle
     *        the handle associated with a DAF
     * @param record
     *        the record number of a particular double precision record within the DAF, whose contents are to be read
     * @param begin
     *        the first word in the specified record to be returned
     * @param end
     *        the final word in the specified record to be returned
     * @param found
     *        (out) true if the specified record was found
     * @return contains the specified portion of the specified record from the specified file
     * @throws PatriusException
     *         if there is a problem in the reading of the file
     */
    public static double[] readContentDPRecord(final int handle, final int record, final int begin, final int end,
                                               final boolean[] found) throws PatriusException {
        double[] data = new double[end - begin + 1];

        // Assume that the record will be found until proven otherwise.
        found[0] = true;

        // First, find the record

        // If the specified handle and record number match those of a buffered record, determine the location of that
        // record within the buffer.
        int bufloc = -1;
        boolean done = false;
        boolean stored = false;

        while (!done) {
            bufloc++;
            stored = (handle == RBHAN[bufloc] && record == RBREC[bufloc]);
            done = (stored || bufloc == rbnbr);
        }

        // If not, determine the location of the least recently requested record (the one with the smallest request
        // number). Get the unit number for the file, and read the record into this location.

        // If an error occurs while reading the record, clear the entire buffer entry in case the entry was corrupted by
        // a partial read.
        // Otherwise, increment the number of reads performed so far.
        if (!stored) {
            bufloc = ArrayUtils.getArrayMinIndex(RBREQ);

            final boolean[] fnd = new boolean[1];
            try {
                data = readDataRecord(handle, record, fnd);
                for (int i = 0; i < NDOUBLES; i++) {
                    RBDAT[i][bufloc] = data[i];
                    data[i] = 0;
                }
            } catch (final IOException e) {
                found[0] = false;
                RBHAN[bufloc] = 0;
                RBREC[bufloc] = 0;
                RBREQ[bufloc] = 0;
            }

            if (!fnd[0]) {
                RBHAN[bufloc] = 0;
                RBREC[bufloc] = 0;
                RBREQ[bufloc] = 0;
            } else {
                if (nRead < Integer.MAX_VALUE) {
                    nRead++;
                }

                RBHAN[bufloc] = handle;
                RBREC[bufloc] = record;

                if (rbnbr < RECORD_BUFFER_SIZE - 1) {
                    rbnbr++;
                }
            }
        }

        // Whether previously stored or just read, the record is now in the buffer. Return the specified portion
        // directly, and increment the corresponding request number.
        if (found[0]) {
            // On input we gave word number (starting at 1). Java starts the arrays at 0.
            // We have to subtract 1 to begin and end
            getResultAndIncrementRequest(data, begin - 1, end - 1, bufloc);
        }

        return data;
    }

    /**
     * Read a summary/descriptor record from a DAF.
     * <p>
     * Based on the ZZDAFGSR routine of the SPICE library
     * </p>
     * 
     * @param handle
     *        the handle associated with the DAF
     * @param record
     *        the record number of a particular summary record within the DAF, whose contents are to be read.
     * @param nd
     *        the number of double precision components in each array summary in the specified file
     * @param ni
     *        the number of integer components in each array summary in the specified file
     * @param fnd
     *        (out) is TRUE when the specified record is found
     * @return contains the contents of the specified record from the DAF associated with HANDLE, properly translated
     * @throws PatriusException
     *         if there is a problem reading the file
     * @throws IOException
     *         if there is a problem managing the file
     */
    private static double[] readSumDescRecord(final int handle, final int record, final int nd, final int ni,
                                              final boolean[] fnd) throws PatriusException, IOException {
        // Assume the data record will not be found, until it has been read from the file, and if necessary,
        // successfully translated.
        fnd[0] = false;

        // Retrieve information regarding the file from the handle manager.
        if (!DafHandleManager.getFound(handle)) {
            throw new PatriusException(PatriusMessages.PDB_UNABLE_TO_LOCATE_FILE, handle);
        }

        final String bff = DafHandleManager.getBinaryFileFormatFromHandle(handle);

        // Now get the file object.
        final File file = DafHandleManager.getFile(handle);
        final RandomAccessFile readFile = new RandomAccessFile(file, READ_ACCESS);
        readFile.seek(DafReaderTools.nRecord2nByte(record));

        // Branch based on whether the binary file format is native or not. Only supported formats can be opened by
        // ZZDDHOPN, so no check of IBFF is required.
        final double[] summary = new double[NDOUBLES];
        if (bff.equals(SpiceCommon.BINARY_FORMAT)) {
            // In the native case, just read the array of double precision numbers from the file. The packed integers
            // will be processed properly by the READ.
            for (int i = 0; i < NDOUBLES; i++) {
                final byte[] bytes = new byte[BYTES_DOUBLE];
                final int j = readFile.read(bytes);
                if (j != BYTES_DOUBLE) {
                    readFile.close();
                    throw new PatriusException(PatriusMessages.PDB_NOT_ENOUGH_BYTES);
                }
                summary[i] = ByteBuffer.wrap(bytes).getDouble();
            }
        } else {
            // We assume the only other option is Little-Endian format
            int kd = nd;
            int ki = ni;
            int ks = 3;
            for (int i = 0; i < NDOUBLES; i++) {
                // If we finished a summary, restart for the following
                if (kd == 0 && ki == 0) {
                    kd = nd;
                    ki = ni;
                }
                final byte[] bytes = new byte[BYTES_DOUBLE];
                final int j = readFile.read(bytes);
                if (j != BYTES_DOUBLE) {
                    readFile.close();
                    throw new PatriusException(PatriusMessages.PDB_NOT_ENOUGH_BYTES);
                }
                if (ks > 0) {
                    // First extract the leading
                    // 3 double precision numbers from the summary record as these
                    // respectively are NEXT, PREV, and NSUM.
                    summary[i] = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
                    ks--;
                } else if (kd > 0) {
                    // First, check to see if there are any double precision numbers to translate.
                    summary[i] = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
                    kd--;
                } else if (ki > 0) {
                    // Check if there are int values to translate.
                    // We will treat each integer individually to not inverse their order. Then we put them together
                    // to create the double that will go to summary.
                    final int i1 = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 4))
                        .order(ByteOrder.LITTLE_ENDIAN).getInt();
                    final int i2 = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 4, BYTES_DOUBLE))
                        .order(ByteOrder.LITTLE_ENDIAN).getInt();
                    final ByteBuffer b = ByteBuffer.allocate(BYTES_DOUBLE)
                        .order(ByteOrder.BIG_ENDIAN).putInt(i1).putInt(i2);
                    b.rewind();
                    summary[i] = b.getDouble();
                    ki -= 2; // On vient de lire 2 integers
                }
            }
        }

        // If we arrived here, everything went fine
        readFile.close();
        fnd[0] = true;
        return summary;
    }

    /**
     * Read a data record from a DAF.
     * <p>
     * Based on ZZDAFGDR from the SPICE library.
     * </p>
     * 
     * @param handle
     *        the handle associated with the DAF
     * @param record
     *        the record number of a particular summary record within the DAF, whose contents are to be read
     * @param fnd
     *        (out) is TRUE when the specified record is found
     * @return contains the contents of the specified record from the DAF associated with HANDLE
     * @throws IOException
     *         if there is a problem managing the file
     * @throws PatriusException
     *         if there is a problem reading the file
     */
    private static double[] readDataRecord(final int handle, final int record, final boolean[] fnd)
        throws IOException, PatriusException {
        // Assume the data record will not be found, until it has been read from the file, and if necessary,
        // successfully translated.
        fnd[0] = false;

        // Retrieve information regarding the file from the handle manager.
        if (!DafHandleManager.getFound(handle)) {
            throw new PatriusException(PatriusMessages.PDB_UNABLE_TO_LOCATE_FILE, handle);
        }

        final String bff = DafHandleManager.getBinaryFileFormatFromHandle(handle);

        // Now get the file object.
        final File file = DafHandleManager.getFile(handle);
        final RandomAccessFile readFile = new RandomAccessFile(file, READ_ACCESS);
        readFile.seek(DafReaderTools.nRecord2nByte(record));

        // Branch based on whether the binary file format is native or not. Only supported formats can be opened by
        // ZZDDHOPN, so no check of IBFF is required.
        final double[] data = new double[NDOUBLES];
        if (bff.equals(SpiceCommon.BINARY_FORMAT)) {
            // In the native case, just read the array of double precision numbers from the file. The packed integers
            // will be processed properly by the READ.
            for (int i = 0; i < NDOUBLES; i++) {
                final byte[] bytes = new byte[BYTES_DOUBLE];
                final int j = readFile.read(bytes);
                if (j != BYTES_DOUBLE) {
                    readFile.close();
                    throw new PatriusException(PatriusMessages.PDB_NOT_ENOUGH_BYTES);
                }
                data[i] = ByteBuffer.wrap(bytes).getDouble();
            }
        } else {
            // We assume the only other option is Little-Endian format
            for (int i = 0; i < NDOUBLES; i++) {
                final byte[] bytes = new byte[BYTES_DOUBLE];
                final int j = readFile.read(bytes);
                if (j != BYTES_DOUBLE) {
                    readFile.close();
                    throw new PatriusException(PatriusMessages.PDB_NOT_ENOUGH_BYTES);
                }
                data[i] = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            }
        }

        // If we arrived here, everything went fine
        readFile.close();
        fnd[0] = true;
        return data;
    }

    /**
     * Getter for the data concerned from RBDAT and increment the request counter.
     * 
     * @param data
     *        (out) Data that we wanted to read from the file
     * @param begin
     *        first component of the buffer that we want
     * @param end
     *        last component of the buffer that we want
     * @param bufloc
     *        position in the buffer desired
     */
    private static void getResultAndIncrementRequest(final double[] data, final int begin, final int end,
                                                     final int bufloc) {
        // Initialization
        final int b = MathLib.max(0, begin);
        final int e = MathLib.min(NDOUBLES, end);

        // Loop on data
        for (int i = b; i <= e; i++) {
            data[i - b] = RBDAT[i][bufloc];
        }

        // Increment the request counter in such a way that integer overflow will not occur. This private module from
        // the handle manager halves RBREQ if adding 1 to NREQ would cause its value to exceed INTMAX.
        if (nReq == Integer.MAX_VALUE) {
            nReq = Integer.MAX_VALUE + 1;
            for (int i = 0; i <= rbnbr; i++) {
                RBREQ[i] = MathLib.max(1, RBREQ[i] / 2);
            }
        } else {
            nReq++;
        }
        RBREQ[bufloc] = nReq;
    }
}
