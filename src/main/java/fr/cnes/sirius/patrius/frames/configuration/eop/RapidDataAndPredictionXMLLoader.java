/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

// CHECKSTYLE: stop NestedBlockDepth check
// Reason: model - Orekit code kept as such

/**
 * Loader for IERS rapid data and prediction file in XML format (finals file).
 * <p>
 * Rapid data and prediction file contain {@link EOPEntry
 * Earth Orientation Parameters} for several years periods, in one file only that is updated regularly.
 * </p>
 * <p>
 * The XML EOP files are recognized thanks to their base names, which must match one of the the patterns
 * <code>finals.2000A.*.xml</code> or <code>finals.*.xml</code> (or the same ending with <code>.gz</code> for
 * gzip-compressed files) where * stands for a word like "all", "daily", or "data".
 * </p>
 * <p>
 * Files containing data (back to 1973) are available at IERS web site: <a
 * href="http://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html">Earth orientation data</a>.
 * </p>
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class RapidDataAndPredictionXMLLoader implements EOP1980HistoryLoader, EOP2000HistoryLoader {

    /** 1000. */
    private static final double ONE_THOUSAND = 1000.;

    /** Conversion factor for milli-arc seconds entries. */
    private static final double MILLI_ARC_SECONDS_TO_RADIANS = Constants.ARC_SECONDS_TO_RADIANS / ONE_THOUSAND;

    /** Conversion factor for milli seconds entries. */
    private static final double MILLI_SECONDS_TO_SECONDS = 1.0 / ONE_THOUSAND;

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** History entries for IAU1980. */
    private EOP1980History history1980;

    /** History entries for IAU2000. */
    private EOP2000History history2000;

    /**
     * Build a loader for IERS XML EOP files.
     * 
     * @param supportedNamesIn
     *        regular expression for supported files names
     */
    public RapidDataAndPredictionXMLLoader(final String supportedNamesIn) {
        super();
        this.supportedNames = supportedNamesIn;
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, PatriusException {
        try {
            // new xml reader instance
            final XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            // set up a reader for line-oriented bulletin B files
            reader.setContentHandler(new EOPContentHandler(name));

            // read all file, ignoring header
            synchronized (this) {
                reader.parse(new InputSource(new InputStreamReader(input, Charset.forName("UTF-8"))));
            }

        } catch (final SAXException se) {
            if ((se.getCause() != null) && (se.getCause() instanceof PatriusException)) {
                throw (PatriusException) se.getCause();
            }
            throw new PatriusException(se, PatriusMessages.SIMPLE_MESSAGE, se.getMessage());
        } catch (final ParserConfigurationException pce) {
            throw new PatriusException(pce, PatriusMessages.SIMPLE_MESSAGE, pce.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void fillHistory(final EOP1980History history) throws PatriusException {
        synchronized (this) {
            this.history1980 = history;
            this.history2000 = null;
            DataProvidersManager.getInstance().feed(this.supportedNames, this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void fillHistory(final EOP2000History history) throws PatriusException {
        synchronized (this) {
            this.history1980 = null;
            this.history2000 = history;
            DataProvidersManager.getInstance().feed(this.supportedNames, this);
        }
    }

    /** Local content handler for XML EOP files. */
    private class EOPContentHandler extends DefaultHandler {

        // elements and attributes used in both daily and finals data files
        /** XML tag. */
        private static final String MJD_ELT = "MJD";
        /** XML tag. */
        private static final String LOD_ELT = "LOD";
        /** XML tag. */
        private static final String X_ELT = "X";
        /** XML tag. */
        private static final String Y_ELT = "Y";
        /** XML tag. */
        private static final String DPSI_ELT = "dPsi";
        /** XML tag. */
        private static final String DEPSILON_ELT = "dEpsilon";
        /** XML tag. */
        private static final String DX_ELT = "dX";
        /** XML tag. */
        private static final String DY_ELT = "dY";

        // elements and attributes specific to daily data files
        /** XML tag. */
        private static final String DATA_EOP_ELT = "dataEOP";
        /** XML tag. */
        private static final String TIME_SERIES_ELT = "timeSeries";
        /** XML tag. */
        private static final String DATE_YEAR_ELT = "dateYear";
        /** XML tag. */
        private static final String DATE_MONTH_ELT = "dateMonth";
        /** XML tag. */
        private static final String DATE_DAY_ELT = "dateDay";
        /** XML tag. */
        private static final String POLE_ELT = "pole";
        /** XML tag. */
        private static final String UT_ELT = "UT";
        /** XML tag. */
        private static final String UT1_U_UTC_ELT = "UT1_UTC";
        /** XML tag. */
        private static final String NUTATION_ELT = "nutation";
        /** XML tag. */
        private static final String SOURCE_ATTR = "source";
        /** XML tag. */
        private static final String BULLETIN_A_SOURCE = "BulletinA";

        // elements and attributes specific to finals data files
        /** XML tag. */
        private static final String FINALS_ELT = "Finals";
        /** XML tag. */
        private static final String DATE_ELT = "date";
        /** XML tag. */
        private static final String EOP_SET_ELT = "EOPSet";
        /** XML tag. */
        private static final String BULLETIN_A_ELT = "bulletinA";
        /** XML tag. */
        private static final String UT1_M_UTC_ELT = "UT1-UTC";

        /** Temporary variable. */
        private boolean inBulletinA;
        /** Temporary variable. */
        private int year;
        /** Temporary variable. */
        private int month;
        /** Temporary variable. */
        private int day;
        /** Temporary variable. */
        private int mjd;
        /** Temporary variable. */
        private double dtu1;
        /** Temporary variable. */
        private double lod;
        /** Temporary variable. */
        private double x;
        /** Temporary variable. */
        private double y;
        /** Temporary variable. */
        private double dpsi;
        /** Temporary variable. */
        private double deps;
        /** Temporary variable. */
        private double dx;
        /** Temporary variable. */
        private double dy;

        /** File name. */
        private final String name;

        /** Buffer for read characters. */
        @SuppressWarnings("PMD.AvoidStringBufferField")
        private final StringBuffer buffer;

        /** Indicator for daily data XML format or final data XML format. */
        private DataFileContent content;

        /**
         * Simple constructor.
         * 
         * @param nameIn
         *        file name
         */
        public EOPContentHandler(final String nameIn) {
            super();
            this.name = nameIn;
            this.buffer = new StringBuffer();
        }

        /** {@inheritDoc} */
        @Override
        public void startDocument() {
            this.content = DataFileContent.UNKNOWN;
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start, final int length) {
            this.buffer.append(ch, start, length);
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName,
                                 final String qName, final Attributes atts) {

            // reset the buffer to empty
            this.buffer.delete(0, this.buffer.length());

            if (this.content == DataFileContent.UNKNOWN) {
                // try to identify file content
                if (qName.equals(TIME_SERIES_ELT)) {
                    // the file contains final data
                    this.content = DataFileContent.DAILY;
                } else if (qName.equals(FINALS_ELT)) {
                    // the file contains final data
                    this.content = DataFileContent.FINAL;
                }
            }

            if (this.content == DataFileContent.DAILY) {
                this.startDailyElement(qName, atts);
            } else if (this.content == DataFileContent.FINAL) {
                this.startFinalElement(qName, atts);
            }

        }

        /**
         * Handle end of an element in a daily data file.
         * 
         * @param qName
         *        name of the element
         * @param atts
         *        element attributes
         */
        private void startDailyElement(final String qName, final Attributes atts) {
            if (qName.equals(TIME_SERIES_ELT)) {
                // reset EOP data
                this.resetEOPData();
            } else if (qName.equals(POLE_ELT) || qName.equals(UT_ELT) || qName.equals(NUTATION_ELT)) {
                final String source = atts.getValue(SOURCE_ATTR);
                if (source != null) {
                    this.inBulletinA = source.equals(BULLETIN_A_SOURCE);
                }
            }
        }

        /**
         * Handle end of an element in a final data file.
         * 
         * @param qName
         *        name of the element
         * @param atts
         *        element attributes
         */
        private void startFinalElement(final String qName, final Attributes atts) {
            if (qName.equals(EOP_SET_ELT)) {
                // reset EOP data
                this.resetEOPData();
            } else if (qName.equals(BULLETIN_A_ELT)) {
                this.inBulletinA = true;
            }
        }

        /**
         * Reset EOP data.
         */
        private void resetEOPData() {
            // Reset EOP data
            // (class variables)
            // Data are updated later
            // No result is returned
            this.inBulletinA = false;
            this.year = -1;
            this.month = -1;
            this.day = -1;
            this.mjd = -1;
            this.dtu1 = Double.NaN;
            this.lod = Double.NaN;
            this.x = Double.NaN;
            this.y = Double.NaN;
            this.dpsi = Double.NaN;
            this.deps = Double.NaN;
            this.dx = Double.NaN;
            this.dy = Double.NaN;
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            try {
                if (this.content == DataFileContent.DAILY) {
                    this.endDailyElement(qName);
                } else if (this.content == DataFileContent.FINAL) {
                    this.endFinalElement(qName);
                }
            } catch (final PatriusException oe) {
                throw new SAXException(oe);
            }
        }

        /**
         * Handle end of an element in a daily data file.
         * 
         * @param qName
         *        name of the element
         * @exception PatriusException
         *            if an EOP element cannot be built
         */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Orekit code kept as such
        private void endDailyElement(final String qName) throws PatriusException {
            // CHECKSTYLE: resume CyclomaticComplexity check

            // Parse data
            if (qName.equals(DATE_YEAR_ELT) && (this.buffer.length() > 0)) {
                // parse the year from the buffer
                this.year = Integer.parseInt(this.buffer.toString());
            } else if (qName.equals(DATE_MONTH_ELT) && (this.buffer.length() > 0)) {
                // parse the month from the buffer
                this.month = Integer.parseInt(this.buffer.toString());
            } else if (qName.equals(DATE_DAY_ELT) && (this.buffer.length() > 0)) {
                // parse the day from the buffer
                this.day = Integer.parseInt(this.buffer.toString());
            } else if (qName.equals(MJD_ELT) && (this.buffer.length() > 0)) {
                // parse the mjd from the buffer
                this.mjd = Integer.parseInt(this.buffer.toString());
            } else if (qName.equals(UT1_M_UTC_ELT)) {
                this.dtu1 = this.overwrite(this.dtu1, 1.0);
            } else if (qName.equals(LOD_ELT)) {
                this.lod = this.overwrite(this.lod, MILLI_SECONDS_TO_SECONDS);
            } else if (qName.equals(X_ELT)) {
                this.x = this.overwrite(this.x, Constants.ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(Y_ELT)) {
                this.y = this.overwrite(this.y, Constants.ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(DPSI_ELT)) {
                this.dpsi = this.overwrite(this.dpsi, MILLI_ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(DEPSILON_ELT)) {
                this.deps = this.overwrite(this.deps, MILLI_ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(DX_ELT)) {
                this.dx = this.overwrite(this.dx, MILLI_ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(DY_ELT)) {
                this.dy = this.overwrite(this.dy, MILLI_ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(POLE_ELT) || qName.equals(UT_ELT) || qName.equals(NUTATION_ELT)) {
                this.inBulletinA = false;
            } else if (qName.equals(DATA_EOP_ELT)) {
                // EOP data
                this.checkDates();
                final boolean cond1 =
                    (!Double.isNaN(this.dtu1)) && (!Double.isNaN(this.lod)) && (!Double.isNaN(this.x));
                final boolean cond2 =
                    (!Double.isNaN(this.y)) && (!Double.isNaN(this.dpsi)) && (!Double.isNaN(this.deps));
                if ((RapidDataAndPredictionXMLLoader.this.history1980 != null) && cond1 && cond2) {
                    // EOP 1980
                    RapidDataAndPredictionXMLLoader.this.history1980.addEntry(new EOP1980Entry(this.mjd, this.dtu1,
                        this.lod, this.x, this.y, this.dpsi, this.deps));
                }
                final boolean cond = (!Double.isNaN(this.dtu1)) && (!Double.isNaN(this.lod)) &&
                        (!Double.isNaN(this.x)) && (!Double.isNaN(this.y));
                if ((RapidDataAndPredictionXMLLoader.this.history2000 != null) && cond) {
                    // EOP 2000
                    RapidDataAndPredictionXMLLoader.this.history2000.addEntry(new EOP2000Entry(this.mjd, this.dtu1,
                        this.lod, this.x, this.y, this.dx, this.dy));
                }
            }
        }

        /**
         * Handle end of an element in a final data file.
         * 
         * @param qName
         *        name of the element
         * @exception PatriusException
         *            if an EOP element cannot be built
         */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Orekit code kept as such
        private void endFinalElement(final String qName) throws PatriusException {
            // CHECKSTYLE: resume CyclomaticComplexity check

            if (qName.equals(DATE_ELT) && (this.buffer.length() > 0)) {
                // Date
                final String[] fields = this.buffer.toString().split("-");
                if (fields.length == 3) {
                    // parse the year from the buffer
                    this.year = Integer.parseInt(fields[0]);
                    // parse the month from the buffer
                    this.month = Integer.parseInt(fields[1]);
                    // parse the day from the buffer
                    this.day = Integer.parseInt(fields[2]);
                }
            } else if (qName.equals(MJD_ELT) && (this.buffer.length() > 0)) {
                // parse the mjd from the buffer
                this.mjd = Integer.parseInt(this.buffer.toString());
            } else if (qName.equals(UT1_U_UTC_ELT)) {
                this.dtu1 = this.overwrite(this.dtu1, 1.0);
            } else if (qName.equals(LOD_ELT)) {
                this.lod = this.overwrite(this.lod, MILLI_SECONDS_TO_SECONDS);
            } else if (qName.equals(X_ELT)) {
                this.x = this.overwrite(this.x, Constants.ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(Y_ELT)) {
                this.y = this.overwrite(this.y, Constants.ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(DPSI_ELT)) {
                this.dpsi = this.overwrite(this.dpsi, MILLI_ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(DEPSILON_ELT)) {
                this.deps = this.overwrite(this.deps, MILLI_ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(DX_ELT)) {
                this.dx = this.overwrite(this.dx, Constants.ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(DY_ELT)) {
                this.dy = this.overwrite(this.dy, Constants.ARC_SECONDS_TO_RADIANS);
            } else if (qName.equals(BULLETIN_A_ELT)) {
                this.inBulletinA = false;
            } else if (qName.equals(EOP_SET_ELT)) {
                // EOP data
                this.checkDates();
                final boolean cond1 =
                    (!Double.isNaN(this.dtu1)) && (!Double.isNaN(this.lod)) && (!Double.isNaN(this.x));
                final boolean cond2 =
                    (!Double.isNaN(this.y)) && (!Double.isNaN(this.dpsi)) && (!Double.isNaN(this.deps));
                if ((RapidDataAndPredictionXMLLoader.this.history1980 != null) && cond1 && cond2) {
                    // EOP 1980
                    RapidDataAndPredictionXMLLoader.this.history1980.addEntry(new EOP1980Entry(this.mjd, this.dtu1,
                        this.lod, this.x, this.y, this.dpsi, this.deps));
                }
                final boolean cond = (!Double.isNaN(this.dtu1)) && (!Double.isNaN(this.lod)) &&
                        (!Double.isNaN(this.x)) && (!Double.isNaN(this.y));
                if ((RapidDataAndPredictionXMLLoader.this.history2000 != null) && cond) {
                    // EOP 2000
                    RapidDataAndPredictionXMLLoader.this.history2000.addEntry(new EOP2000Entry(this.mjd, this.dtu1,
                        this.lod, this.x, this.y, this.dx, this.dy));
                }
            }
        }

        /**
         * Overwrite a value if it is not set or if we are in a bulletinB.
         * 
         * @param oldValue
         *        old value to overwrite (may be NaN)
         * @param factor
         *        multiplicative factor to apply to raw read data
         * @return a new value
         */
        private double overwrite(final double oldValue, final double factor) {
            final double res;
            if (this.buffer.length() == 0) {
                // there is nothing to overwrite with
                res = oldValue;
            } else if (this.inBulletinA && (!Double.isNaN(oldValue))) {
                // the value is already set and bulletin A values have a low priority
                res = oldValue;
            } else {
                // either the value is not set or it is a high priority bulletin B value
                res = Double.parseDouble(this.buffer.toString()) * factor;
            }
            return res;
        }

        /**
         * Check if the year, month, day date and MJD date are consistent.
         * 
         * @exception PatriusException
         *            if dates are not consistent
         */
        private void checkDates() throws PatriusException {
            if (new DateComponents(this.year, this.month, this.day).getMJD() != this.mjd) {
                throw new PatriusException(PatriusMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                    this.name, this.year, this.month, this.day, this.mjd);
            }
        }

    }

    /** Enumerate for data file content. */
    private static enum DataFileContent {

        /** Unknown content. */
        UNKNOWN,

        /** Daily data. */
        DAILY,

        /** Final data. */
        FINAL;

    }

    // CHECKSTYLE: resume NestedBlockDepth check
}
