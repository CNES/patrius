/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
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
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class representing a Poisson series for nutation or ephemeris computations.
 * <p>
 * A Poisson series is composed of a time polynomial part and a non-polynomial part which consist in summation series.
 * The {@link SeriesTerm series terms} are harmonic functions (combination of sines and cosines) of polynomial
 * <em>arguments</em>. The polynomial arguments are combinations of luni-solar or planetary {@link BodiesElements
 * elements}.
 * </p>
 * 
 * @author Luc Maisonobe
 * @see SeriesTerm
 */
@SuppressWarnings("PMD.NullAssignment")
public class PoissonSeries implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -3016824169123970737L;
    
    /** Constant for parsing Poisson series term */
    private static final int CONST7 = 7;
    
    /** Constant for parsing Poisson series term */
    private static final int CONST8 = 8;
    
    /** Constant for parsing Poisson series term */
    private static final int CONST9 = 9;
    
    /** Constant for parsing Poisson series term */
    private static final int CONST10 = 10;

    /** Coefficients of the polynomial part. */
    private double[] coefficients;

    /** Non-polynomial series. */
    private SeriesTerm[][] series;

    /**
     * Build a Poisson series from an IERS table file.
     * 
     * @param stream
     *        stream containing the IERS table
     * @param factor
     *        multiplicative factor to use for coefficients
     * @param name
     *        name of the resource file (for error messages only)
     * @exception PatriusException
     *            if stream is null or the table cannot be parsed
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public PoissonSeries(final InputStream stream, final double factor, final String name) throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        if (stream == null) {
            throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_FILE, name);
        }

        try {
            // the polynomial part should read something like:
            // -16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5
            // or something like:
            // 0''.014506 + 4612''.15739966t + 1''.39667721t^2 - 0''.00009344t^3 + 0''.00001882t^4
            final Pattern termPattern =
                Pattern.compile("\\p{Space}*([-+]?)" +
                    "\\p{Space}*(\\p{Digit}+)(?:'')?(\\.\\p{Digit}+)" +
                    "(?:\\p{Space}*t(?:\\^\\p{Digit}+)?)?");

            // setup the reader
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line = reader.readLine();
            int lineNumber = 1;

            // look for the polynomial part
            while (line != null) {
                if (this.parsePolynomial(termPattern.matcher(line), factor)) {
                    // we have parsed the polynomial part
                    line = null;
                } else {
                    // we are still in the header
                    line = reader.readLine();
                    ++lineNumber;
                }
            }
            if (this.coefficients == null) {
                throw new PatriusException(PatriusMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }

            line = reader.readLine();
            ++lineNumber;

            // the series parts should read something like:
            // j = 0 Nb of terms = 1306
            //
            // 1 -6844318.44 1328.67 0 0 0 0 1 0 0 0 0 0 0 0 0 0
            // 2 -523908.04 -544.76 0 0 2 -2 2 0 0 0 0 0 0 0 0 0
            // 3 -90552.22 111.23 0 0 2 0 2 0 0 0 0 0 0 0 0 0
            // 4 82168.76 -27.64 0 0 0 0 2 0 0 0 0 0 0 0 0 0
            final Pattern seriesHeaderPattern =
                Pattern.compile("^\\p{Space}*j\\p{Space}*=\\p{Space}*(\\p{Digit}+)" +
                    ".*=\\p{Space}*(\\p{Digit}+)\\p{Space}*$");

            // look for the non-polynomial part
            final List<SeriesTerm[]> array = new ArrayList<>();
            while (line != null) {
                final int nTerms = parseSeriesHeader(seriesHeaderPattern.matcher(line),
                    array.size(), name, lineNumber);
                if (nTerms >= 0) {
                    // we have found a non-polynomial series

                    // skip blank lines
                    line = reader.readLine();
                    ++lineNumber;
                    while ((line != null) && (line.trim().isEmpty())) {
                        line = reader.readLine();
                        ++lineNumber;
                    }

                    // read the terms of the current serie
                    final SeriesTerm[] serie = new SeriesTerm[nTerms];
                    for (int i = 0; i < nTerms; ++i) {
                        serie[i] = parseSeriesTerm(line, factor, name, lineNumber);
                        line = reader.readLine();
                        ++lineNumber;
                    }

                    // the serie has been completed, store it
                    array.add(serie);

                } else {
                    // we are still in the intermediate lines
                    line = reader.readLine();
                    ++lineNumber;
                }
            }

            if (array.isEmpty()) {
                throw new PatriusException(PatriusMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }

            // store the non-polynomial part series
            this.series = array.toArray(new SeriesTerm[array.size()][]);

        } catch (final IOException ioe) {
            throw new PatriusException(ioe, new DummyLocalizable(ioe.getMessage()));
        }

    }

    /**
     * Parse a polynomial description line.
     * 
     * @param termMatcher
     *        matcher for the polynomial terms
     * @param factor
     *        multiplicative factor to use for coefficients
     * @return true if the line was parsed successfully
     */
    private boolean parsePolynomial(final Matcher termMatcher, final double factor) {

        // parse the polynomial one polynomial term after the other
        if (!termMatcher.lookingAt()) {
            return false;
        }

        // store the concatenated sign, integer and fractional parts of the monomial coefficient
        final List<String> coeffs = new ArrayList<>();
        do {
            coeffs.add(termMatcher.group(1) + termMatcher.group(2) + termMatcher.group(3));
        } while (termMatcher.find());

        // parse the coefficients
        this.coefficients = new double[coeffs.size()];
        for (int i = 0; i < this.coefficients.length; ++i) {
            this.coefficients[i] = factor * Double.parseDouble(coeffs.get(i));
        }

        return true;

    }

    /**
     * Parse a series header line.
     * 
     * @param headerMatcher
     *        matcher for the series header line
     * @param expected
     *        expected series index
     * @param name
     *        name of the resource file (for error messages only)
     * @param lineNumber
     *        line number (for error messages only)
     * @return the number of terms in the series (-1 if the line
     *         cannot be parsed)
     * @exception PatriusException
     *            if the header does not match
     *            the expected series number
     */
    private static int parseSeriesHeader(final Matcher headerMatcher, final int expected,
                                  final String name, final int lineNumber) throws PatriusException {

        // is this a series header line ?
        if (!headerMatcher.matches()) {
            return -1;
        }

        // sanity check
        if (Integer.parseInt(headerMatcher.group(1)) != expected) {
            throw new PatriusException(PatriusMessages.MISSING_SERIE_J_IN_FILE,
                expected, name, lineNumber);
        }

        return Integer.parseInt(headerMatcher.group(2));

    }

    /**
     * Parse a series term line.
     * 
     * @param line
     *        data line to parse
     * @param factor
     *        multiplicative factor to use for coefficients
     * @param name
     *        name of the resource file (for error messages only)
     * @param lineNumber
     *        line number (for error messages only)
     * @return a series term
     * @exception PatriusException
     *            if the line is null or cannot be parsed
     */
    private static SeriesTerm parseSeriesTerm(final String line, final double factor,
                                       final String name, final int lineNumber) throws PatriusException {

        // sanity check
        if (line == null) {
            throw new PatriusException(PatriusMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                name, lineNumber - 1);
        }

        // parse the Poisson series term
        final String[] fields = line.split("\\p{Space}+");
        final int l = fields.length;
        if (((l == CONST10 + CONST7) && (fields[0].length() != 0)) ||
            ((l == CONST10 + CONST8) && (fields[0].length() == 0))) {
            return SeriesTerm.buildTerm(Double.parseDouble(fields[l - CONST10 - 6]) * factor,
                Double.parseDouble(fields[l - CONST10 - 5]) * factor,
                Integer.parseInt(fields[l - CONST10 - 4]), Integer.parseInt(fields[l - CONST10 - 3]),
                Integer.parseInt(fields[l - CONST10 - 2]), Integer.parseInt(fields[l - CONST10 - 1]),
                Integer.parseInt(fields[l - CONST10]), Integer.parseInt(fields[l - CONST9]),
                Integer.parseInt(fields[l - CONST8]), Integer.parseInt(fields[l - CONST7]),
                Integer.parseInt(fields[l - 6]), Integer.parseInt(fields[l - 5]),
                Integer.parseInt(fields[l - 4]), Integer.parseInt(fields[l - 3]),
                Integer.parseInt(fields[l - 2]), Integer.parseInt(fields[l - 1]));
        }

        throw new PatriusException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
            lineNumber, name, line);

    }

    /**
     * Compute the value of the development for the current date.
     * 
     * @param t
     *        current date
     * @param elements
     *        luni-solar and planetary elements for the current date
     * @return current value of the development
     */
    public double value(final double t, final BodiesElements elements) {

        // private implementation
        final double[] irez = this.valueImpl(t, elements, null, false);

        return irez[0];

    }

    /**
     * Compute the value of the development for the current date and its first time derivative.
     * 
     * @param t
     *        current date
     * @param elements
     *        luni-solar and planetary elements for the current date
     * @param elementsP
     *        luni-solar and planetary time derivative elements for the current date
     * @return current value of the development
     */
    public double[] value(final double t, final BodiesElements elements, final BodiesElements elementsP) {
        return this.valueImpl(t, elements, elementsP, true);

    }

    /**
     * Compute the value of the development for the current date,
     * and its first time derivative if needed.
     * 
     * @param t
     *        current date
     * @param elements
     *        luni-solar and planetary elements for the current date
     * @param elementsP
     *        luni-solar and planetary time derivative elements for the current date
     * @param deriv
     *        true if time derivative needed
     * @return current value of the development
     */
    private double[] valueImpl(final double t, final BodiesElements elements,
                               final BodiesElements elementsP, final boolean deriv) {

        // polynomial part
        double p = 0;
        // time derivative polynomial part
        double pp = 0;
        for (int i = this.coefficients.length - 1; i >= 0; --i) {
            if (deriv) {
                pp = pp * t + p / Constants.JULIAN_CENTURY;
            }
            p = p * t + this.coefficients[i];
        }
        // non-polynomial part
        double np = 0;
        // time derivative non-polynomial part
        double npp = 0;
        for (int i = this.series.length - 1; i >= 0; --i) {

            final SeriesTerm[] serie = this.series[i];

            // add the harmonic terms starting from the last (smallest) terms,
            // to avoid numerical problems
            double s = 0;
            double sp = 0;
            for (int k = serie.length - 1; k >= 0; --k) {
                if (deriv) {
                    final double[] tmp = serie[k].value(elements, elementsP);
                    s += tmp[0];
                    sp += tmp[1];
                } else {
                    s += serie[k].value(elements);
                }
            }

            if (deriv) {
                npp = npp * t + np / Constants.JULIAN_CENTURY + sp;
            }
            np = np * t + s;

        }

        // add the polynomial and the non-polynomial parts
        return new double[] { p + np, pp + npp };

    }

}
