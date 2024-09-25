/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history creation 21/06/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.cache.CacheEntry;
import fr.cnes.sirius.patrius.tools.cache.FIFOThreadSafeCache;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Reader for the USK data file (file of the "NEWUSK" type).
 * Note : the code is ported from Fortran and not optimized.
 * Since the file is not big, and its format is not meant to change often,
 * it was decided an optimization pass was not needed.
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment the instance is mutable, but no longer changes after the first call to getData() (the first call
 *                      triggers the DataProvidersManager on the instance).
 *                      This means : an instance can be shared and used in several threads, if getData() is called once
 *                      in a single thread context first.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public final class USKLoader implements USKProvider, DataLoader {

    /** Serializable UID. */
    private static final long serialVersionUID = 4885039372894263006L;
    /** "\\s+" */
    private static final String READST = "\\s+";
    /** 9 */
    private static final int Q9 = 9;
    /** 10 */
    private static final int Q10 = 10;
    /** 11 */
    private static final int Q11 = 11;
    /** 12 */
    private static final int Q12 = 12;
    /** 13 */
    private static final int Q13 = 13;
    /** 14 */
    private static final int Q14 = 14;
    /** 24 */
    private static final int Q24 = 24;
    /** 25 */
    private static final int Q25 = 25;
    /** 28 */
    private static final int Q28 = 28;
    /** 49 */
    private static final int Q49 = 49;
    /** 76 */
    private static final int Q76 = 76;
    /** 100 */
    private static final int Q100 = 100;

    /** entries number expected in the file */
    private static final int ENTRIESNBR = Q28 + 3 * 9 * Q49 + 3 * Q13 * Q76;

    /** file name */
    private final String fileNameIn;

    /** still accepting data */
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private volatile boolean stillAccept;

    /** the line, as doubles */
    private final double[] doutab = new double[ENTRIESNBR * Q12];

    /** the line, as integers */
    private final int[] inttab = new int[ENTRIESNBR * Q12];

    /** Cache for the abciuf (0) abcium (1) arrays */
    private final FIFOThreadSafeCache<Integer, double[][][][]> abciufAbciumCache;

    /** Cache for the uskData */
    private final FIFOThreadSafeCache<double[], USKData> uskDataCache;

    /**
     * Creates a USK data file reader and load the file.
     * 
     * @param fileName
     *        name of the file
     */
    public USKLoader(final String fileName) {
        this.stillAccept = true;
        this.fileNameIn = fileName;

        this.abciufAbciumCache = new FIFOThreadSafeCache<>();
        this.uskDataCache = new FIFOThreadSafeCache<>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return this.stillAccept;
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input,
                         final String name)
        throws IOException, PatriusException {
        // buffer file data
        final InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);

        final BufferedReader r = new BufferedReader(reader);

        // reading the first (and only useful) line
        final String firstLine = r.readLine();
        if (firstLine == null) {
            throw new PatriusException(PatriusMessages.PDB_USK_FILE_ERROR, "wrong file");
        }
        // splitting the line
        final String[] tab = firstLine.trim().split(READST);

        // parse all fields as doubles
        for (int i = 0; i < tab.length; i++) {
            try {
                this.doutab[i] = Double.parseDouble(tab[i]);
            } catch (final NumberFormatException e) {
                // The field is not a number.
                // Note : catching a NumberFormatException is suboptimal,
                // but the non-numbers are sparse in the file,
                // so it's not a problem in practice.
                this.doutab[i] = Double.NEGATIVE_INFINITY;
            }
            // Also, integers
            this.inttab[i] = (int) MathLib.round(this.doutab[i]);
        }

        this.stillAccept = false;
    }

    /**
     * Last operations of the data loading that can't be done without knowing
     * the context (R12 value);
     * 
     * @param r12
     *        R12 constant value.
     * @param uf
     *        reference to uf in caller
     * @param um
     *        reference to um in caller
     * @param abciuf
     *        reference to abciuf in caller
     * @param abcium
     *        reference to abcium in caller
     */
    private static void uskLastEntries(final double r12,
                                final double[][] uf, final double[][] um,
                                final double[][][] abciuf, final double[][][] abcium) {

        // UF data filling from ABCIUF
        for (int j = 0; j < Q76; j++) {
            for (int k = 0; k < Q13; k++) {
                // compute UF values from ABCIUF
                uf[k][j] = abciuf[k][j][0] * r12 * r12 + abciuf[k][j][1] * r12 + abciuf[k][j][2];
            }
        }

        // UM data filling from ABCIUM
        for (int j = 0; j < Q49; j++) {
            for (int k = 0; k < Q9; k++) {
                // compute UM values from ABCIUM
                um[k][j] = abcium[k][j][0] * r12 * r12 + abcium[k][j][1] * r12 + abcium[k][j][2];
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public USKData getData(final AbsoluteDate date, final double r12) throws PatriusException {

        // first call : file loading
        if (this.stillAccept) {
            DataProvidersManager.getInstance().feed(this.fileNameIn, this);
        }

        // check if the data has been loaded
        if (this.stillAccept) {
            // The data could not be loaded.
            throw new PatriusException(PatriusMessages.PDB_USK_FILE_ERROR, "no file");
        }

        // date
        final DateTimeComponents components = date.getComponents(TimeScalesFactory.getTT());
        final int mon = components.getDate().getMonth();
        final int ian = components.getDate().getYear();

        // first level of cache: same year, same month, same r12
        final double[] key = new double[] { ian, mon, r12 };
        return this.uskDataCache.computeIf(entry -> {
            final double[] internalKey = entry.getKey();
            // Condition to match an existent cache key
            return internalKey[0] == ian && internalKey[1] == mon && internalKey[2] == r12;
        },
            // Lambda to build a new uskData in case no key matched the year and month and r12
            () -> {
                final int posInit = ENTRIESNBR * (mon - 1);

                // Initializations
                final int[] iuf = new int[Q14];
                final int[] ium = new int[Q14];

                final double[][] um = new double[Q9][Q49];
                final double[][] uf = new double[Q13][Q76];

                // data filling
                // IUF and IUM filling
                for (int i = 0; i < Q10; i++) {
                    iuf[i] = this.inttab[i + posInit];
                    ium[i] = this.inttab[i + Q14 + posInit];
                }
                // IUF and IUM particular values
                iuf[Q11] = this.inttab[Q10 + posInit];
                iuf[Q12] = this.inttab[Q11 + posInit];
                ium[Q11] = this.inttab[Q24 + posInit];
                ium[Q12] = this.inttab[Q25 + posInit];

                // Second level of cache: same month
                // This second cache level is necessary if the r12 provider is not at least a piecewise constant
                // provider. In this case, the first cache is inefficient.
                final CacheEntry<Integer, double[][][][]> cacheEntry = this.abciufAbciumCache.computeIfAbsent(mon,
                    // Lambda to build abciuf and abcium that only depend on the month (not year or r12)
                    () -> {
                        final double[][][] abciuf = new double[Q13][Q76][3];
                        final double[][][] abcium = new double[Q9][Q49][3];

                        int indLec = Q28 + posInit;

                        // ABCIUM temporary data filling
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < Q49; j++) {
                                for (int k = 0; k < Q9; k++) {
                                    abcium[k][j][i] = this.doutab[indLec];
                                    indLec++;
                                }
                            }
                        }

                        // ABCIUF temporary data filling
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < Q76; j++) {
                                for (int k = 0; k < Q13; k++) {
                                    abciuf[k][j][i] = this.doutab[indLec];
                                    indLec++;
                                }
                            }
                        }

                        return new CacheEntry<>(mon, new double[][][][] { abciuf, abcium });
                    });

                final double[][][][] abciufAbcium = cacheEntry.getValue();

                // computation of the UF and UM values
                USKLoader.uskLastEntries(r12, uf, um, abciufAbcium[0], abciufAbcium[1]);

                // IUF and IUM particular values
                iuf[Q13] = (int) (r12 * Q10);
                ium[Q13] = iuf[Q13];
                iuf[Q10] = ian * Q100 + mon;
                ium[Q10] = iuf[Q10];

                return new CacheEntry<>(key, new USKData(iuf, ium, uf, um));
            }).getValue();
    }
}
