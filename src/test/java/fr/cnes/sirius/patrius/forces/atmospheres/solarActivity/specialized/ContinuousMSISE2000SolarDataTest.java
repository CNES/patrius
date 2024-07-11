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
 * @history Created 28/10/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:Created new MISISE2000 solar data class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataReader;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCScale;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Test class for {@link ContinuousMSISE2000SolarData}
 * 
 * @author Rmai Houdroge
 * @since 2.1
 * @version $$
 * 
 */
public class ContinuousMSISE2000SolarDataTest {

    private SolarActivityDataReader data;
    private UTCScale utc;
    private ContinuousMSISE2000SolarData msis2000Data;
    private final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
    private double[][] refData;

    /**
     * Features description.
     */
    public enum features {
        /**
         * @featureTitle MSISE2000 solar activity
         * 
         * @featureDescription here we test the methods of the MSISE2000 solar activity toolbox
         * 
         * @coveredRequirements DV-MOD_261
         */
        MSISE2000_SOLAR_ACTIVITY
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#MSISE2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ContinuousMSISE2000SolarData#getApValues(AbsoluteDate)}.
     * 
     * @description make sure the correct ap value are return by these method
     * 
     * @input date
     * 
     * @output ap
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void testNewData() throws PatriusException {

        Utils.setDataRoot("solarData");

        this.utc = TimeScalesFactory.getUTC();

        final double[][] ref = {
            { 17531.9895073301850000, 101.7997567899292300, 96.2070259072198440, 4.1354926698149939,
                2.4160586414800491, 2.0000000000000000, 2.0000000000000000, 3.1678827170399018,
                14.2718973774099140, 9.3111319433301105 },
            { 17531.9903983864640000, 101.8026081700227200, 96.2069188961946370, 4.1346016135357786,
                2.4231870917137712, 2.0000000000000000, 2.0000000000000000, 3.1536258165724576,
                14.2594225895009000, 9.3271709563559853 },
            { 17531.9912352686150000, 101.8052861929056300, 96.2068183269180960, 4.1337647313848720,
                2.4298821489210241, 2.0000000000000000, 2.0000000000000000, 3.1402357021579519,
                14.2477062393882080, 9.3422348350723041 },
            { 17531.9920248152120000, 101.8078127420158100, 96.2067233889470830, 4.1329751847879379,
                2.4361985216964968, 2.0000000000000000, 2.0000000000000000, 3.1276029566070065,
                14.2366525870311310, 9.3564466738171177 },
            { 17531.9927730160960000, 101.8102069848449800, 96.2066333714005280, 4.1322269839038199,
                2.4421841287694406, 2.0000000000000000, 2.0000000000000000, 3.1156317424611188,
                14.2261777746534790, 9.3699142897312413 },
            { 17531.9934851467590000, 101.8124858029652400, 96.2065476473146930, 4.1315148532412422,
                2.4478811740700621, 2.0000000000000000, 2.0000000000000000, 3.1042376518598758,
                14.2162079453773910, 9.3827326416576398 },
            { 17531.9941658812420000, 101.8146641533123400, 96.2064656604628250, 4.1308341187577753,
                2.4533270499377977, 2.0000000000000000, 2.0000000000000000, 3.0933459001244046,
                14.2066776626088540, 9.3949858623600448 },
            { 17531.9948193874520000, 101.8167553731822300, 96.2063869141992570, 4.1301806125484291,
                2.4585550996125676, 2.0000000000000000, 2.0000000000000000, 3.0828898007748649,
                14.1975285756780070, 9.4067489741282770 },
            { 17531.9954494080450000, 101.8187714390805900, 96.2063109619681430, 4.1295505919551942,
                2.4635952643584460, 2.0000000000000000, 2.0000000000000000, 3.0728094712831080,
                14.1887082873727190, 9.4180893448065035 },
            { 17531.9960593295360000, 101.8207231878535700, 96.2062373991754920, 4.1289406704636349,
                2.4684746362909209, 2.0000000000000000, 2.0000000000000000, 3.0630507274181582,
                14.1801693864908880, 9.4290679316545720 },
            { 17531.9966522417640000, 101.8226205069804600, 96.2061658561780600, 4.1283477582364867,
                2.4732179341081064, 2.0000000000000000, 2.0000000000000000, 3.0535641317837872,
                14.1718686153108140, 9.4397403517432394 },
            { 17531.9972309895200000, 101.8244724998017700, 96.2060959921808210, 4.1277690104798239,
                2.4778479161614086, 2.0000000000000000, 2.0000000000000000, 3.0443041676771827,
                14.1637661467175350, 9.4501578113631695 },
            { 17531.9977982178790000, 101.8262876305496300, 96.2060274898666560, 4.1272017821211193,
                2.4823857430310454, 2.0000000000000000, 2.0000000000000000, 3.0352285139379092,
                14.1558249496956710, 9.4603679218198522 },
            { 17531.9983564124630000, 101.8280738532193900, 96.2059600506116650, 4.1266435875368188,
                2.4868512997054495, 2.0000000000000000, 2.0000000000000000, 3.0262974005891010,
                14.1480102255154630, 9.4704154243372614 },
            { 17531.9989079357290000, 101.8298387276707200, 96.2058933901621030, 4.1260920642707788,
                2.4912634858337697, 2.0000000000000000, 2.0000000000000000, 3.0174730283324607,
                14.1402888997909030, 9.4803428431259817 },
            { 17531.9994550602860000, 101.8315895262523600, 96.2058272346518070, 4.1255449397140183,
                2.4956404822878540, 2.0000000000000000, 2.0000000000000000, 3.0087190354242921,
                14.1326291559962560, 9.4901910851476714 },
            { 17532.0000000000000000, 101.8333333333372100, 96.2057613168722410, 4.1250000000000000,
                2.5000000000000000, 2.0000000000000000, 2.0000000000000000, 3.0000000000000000,
                14.1250000000000000, 9.5000000000000000 },
            { 17532.0005449397140000, 101.8350771404220700, 96.2056953726963260, 4.1244550602859817,
                2.5043595177121460, 2.0000000000000000, 2.0000000000000000, 2.9912809645757079,
                14.1190056631457990, 9.5081740957102738 },
            { 17532.0010920642710000, 101.8368279390037000, 96.2056291375731260, 4.1239079357292212,
                2.5087365141662303, 2.0000000000000000, 2.0000000000000000, 2.9825269716675393,
                14.1129872930214330, 9.5163809640616819 },
            { 17532.0016435875370000, 101.8385928134550400, 96.2055623430101720, 4.1233564124631812,
                2.5131487002945505, 2.0000000000000000, 2.0000000000000000, 2.9737025994108990,
                14.1069205370949930, 9.5246538130522822 },
            { 17532.0022017821210000, 101.8403790361247900, 96.2054947129583270, 4.1227982178788807,
                2.5176142569689546, 2.0000000000000000, 2.0000000000000000, 2.9647714860620908,
                14.1007803966676870, 9.5330267318167898 },
            { 17532.0027690104800000, 101.8421941668726400, 96.2054259600153190, 4.1222309895201761,
                2.5221520838385914, 2.0000000000000000, 2.0000000000000000, 2.9556958323228173,
                14.0945408847219370, 9.5415351571973588 },
            { 17532.0033477582360000, 101.8440461596939700, 96.2053557813455310, 4.1216522417635133,
                2.5267820658918936, 2.0000000000000000, 2.0000000000000000, 2.9464358682162128,
                14.0881746593986460, 9.5502163735473005 },
            { 17532.0039406704640000, 101.8459434788208400, 96.2052838542237940, 4.1210593295363651,
                2.5315253637090791, 2.0000000000000000, 2.0000000000000000, 2.9369492725818418,
                14.0816526249000160, 9.5591100569545233 },
            { 17532.0045505919550000, 101.8478952275938400, 96.2052098310752800, 4.1204494080448058,
                2.5364047356415540, 2.0000000000000000, 2.0000000000000000, 2.9271905287168920,
                14.0749434884928630, 9.5682588793279137 },
            { 17532.0051806125480000, 101.8499112934921800, 96.2051333338789050, 4.1198193874515709,
                2.5414449003874324, 2.0000000000000000, 2.0000000000000000, 2.9171101992251351,
                14.0680132619672800, 9.5777091882264358 },
            { 17532.0058341187580000, 101.8520025133620900, 96.2050539477757520, 4.1191658812422247,
                2.5466729500622023, 2.0000000000000000, 2.0000000000000000, 2.9066540998755954,
                14.0608246936644720, 9.5875117813666293 },
            { 17532.0065148532410000, 101.8541808637091900, 96.2049712136908970, 4.1184851467587578,
                2.5521188259299379, 2.0000000000000000, 2.0000000000000000, 2.8957623481401242,
                14.0533366143463350, 9.5977227986186335 },
            { 17532.0072269839040000, 101.8564596818294300, 96.2048846197398570, 4.1177730160961801,
                2.5578158712305594, 2.0000000000000000, 2.0000000000000000, 2.8843682575388812,
                14.0455031770579810, 9.6084047585572989 },
            { 17532.0079751847880000, 101.8588539246586200, 96.2047935911465400, 4.1170248152120621,
                2.5638014783035032, 2.0000000000000000, 2.0000000000000000, 2.8723970433929935,
                14.0372729673326830, 9.6196277718190686 },
            { 17532.0087647313850000, 101.8613804737688000, 96.2046974783360720, 4.1162352686151280,
                2.5701178510789759, 2.0000000000000000, 2.0000000000000000, 2.8597642978420481,
                14.0285879547664080, 9.6314709707730799 },
            { 17532.0096016135360000, 101.8640584966517100, 96.2045955427958570, 4.1153983864642214,
                2.5768129082862288, 2.0000000000000000, 2.0000000000000000, 2.8463741834275424,
                14.0193822511064350, 9.6440242030366790 },
            { 17532.0104926698150000, 101.8669098767452000, 96.2044869402028840, 4.1145073301850061,
                2.5839413585199509, 2.0000000000000000, 2.0000000000000000, 2.8321172829600982,
                14.0095806320350680, 9.6573900472249079 }
        };

        final SolarActivityDataProvider prov = SolarActivityDataFactory.getSolarActivityDataProvider();
        final ContinuousMSISE2000SolarData data = new ContinuousMSISE2000SolarData(prov);

        final AbsoluteDate refDate = AbsoluteDate.FIFTIES_EPOCH_UTC;

        double iflux;
        double mflux;
        double[] ap;

        for (final double[] element : ref) {
            final AbsoluteDate date = new AbsoluteDate(refDate, element[0] * Constants.JULIAN_DAY, this.utc);

            iflux = data.getInstantFlux(date);
            mflux = data.getMeanFlux(date);
            ap = data.getApValues(date);

            Assert.assertEquals(0, (iflux - element[1]) / element[1], 1e-13);
            Assert.assertEquals(0, (mflux - element[2]) / element[2], 1e-13);

            for (int j = 0; j < ap.length; j++) {
                Assert.assertEquals(0, (ap[j] - element[3 + j]) / element[3 + j], 1e-11);
            }

        }

    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#MSISE2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ContinuousMSISE2000SolarData#getInstantFlux(AbsoluteDate)}.
     * 
     * @description make sure the correct instant flux value are return by these method
     * 
     * @input date
     * 
     * @output ap
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void testGetInstantFlux() throws PatriusException {
        try {
            this.data.getInstantFluxValue(this.format(5844, 61000));
            fail();
        } catch (final PatriusExceptionWrapper e) {
            // expected!
        }

        double x, expected, dep, actual;

        for (int i = 0; i < 11; i++) {

            x = 86400 * i / 10;
            expected = (82 * (86400 - x) + 78.9 * x) / 86400;
            dep = x / (86400 - 61200) > 1 ? 1 : 0;
            actual =
                this.msis2000Data.getInstantFlux(this.format(5844 + dep, dep == 1 ? x - (86400 - 61200) : 61200 + x)
                    .shiftedBy(Constants.JULIAN_DAY));

            assertEquals(0, (actual - expected) / actual, this.eps);
        }

    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#MSISE2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ContinuousMSISE2000SolarData#getMeanFlux(AbsoluteDate)}.
     * 
     * @description make sure the correct mean flux value are return by these method
     * 
     * @input date
     * 
     * @output mean flux
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void testGetMeanFlux() throws PatriusException {

        // date : 18211 72000
        final AbsoluteDate ref = this.format(18211, 72000);
        ref.shiftedBy(-81 / 2. * 86400);
        ref.shiftedBy(+81 / 2. * 86400);

        AbsoluteDate previous, current;
        double area = 0.;
        for (int i = 1; i < this.refData.length; i++) {
            previous = this.format(this.refData[i - 1][0], this.refData[i - 1][1]);
            current = this.format(this.refData[i][0], this.refData[i][1]);

            area += (this.refData[i - 1][2] + this.refData[i][2]) * current.durationFrom(previous) / 2;
        }

        final double expected = area / (81. * 86400);
        final double actual = this.msis2000Data.getMeanFlux(ref);

        assertEquals(0, (actual - expected) / actual, this.eps);
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#MSISE2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ContinuousMSISE2000SolarData#getMinDate()}.
     * 
     * @description make sure the min date is returned by these method
     * 
     * @input
     * 
     * @output mind ate
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void testGetMinDate() {
        // 5844 61200
        Assert.assertEquals(0, this.format(5844, 61200).offsetFrom(this.data.getMinDate(), this.utc), this.eps);
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#MSISE2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ContinuousMSISE2000SolarData#getMaxDate()}.
     * 
     * @description make sure the min date is returned by these method
     * 
     * @input
     * 
     * @output mind ate
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void testGetMaxDate() {
        // 22704 72000
        Assert.assertEquals(0, this.format(22704, 72000).offsetFrom(this.data.getMaxDate(), this.utc), this.eps);
    }

    /**
     * Return the date from file
     * 
     * @param days
     *        days
     * @param seconds
     *        seconds
     * @return date
     */
    private AbsoluteDate format(final double days, final double seconds) {
        final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00), days * Constants.JULIAN_DAY + seconds);
        return new AbsoluteDate(currentDateTime, this.utc);
    }

    // Utility method
    // /**
    // * Return the days and seconds
    // *
    // * @param days
    // * days
    // * @param seconds
    // * seconds
    // * @return date
    // * @throws OrekitException
    // */
    // private double[] deformat(final AbsoluteDate date) throws OrekitException {
    // final DateTimeComponents ref = new DateTimeComponents(new DateTimeComponents(
    // DateComponents.FIFTIES_EPOCH, TimeComponents.H00), 0);
    //
    // final double total = date.offsetFrom(new AbsoluteDate(ref, utc), TimeScalesFactory.getUTC());
    //
    // final double days = FastMath.floor(total / 86400.);
    // final double sec = total - days * 86400.;
    // return new double[] { days, sec };
    // }

    /**
     * set up test case
     * 
     * @throws PatriusException
     *         if no data loaded
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("atmosphere");
        this.utc = TimeScalesFactory.getUTC();
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader(
            SolarActivityDataFactory.ACSOL_FILENAME));
        this.data = (SolarActivityDataReader) SolarActivityDataFactory.getSolarActivityDataProvider();

        this.msis2000Data = new ContinuousMSISE2000SolarData(this.data);

        this.refData = new double[][] {
            { 18171, 28800, this.data.getInstantFluxValue(this.format(18171, 28800)), 0, 0, 0, 0, 0, 0, 0, 0 },
            { 18171, 72000, 126.3, 15, 18, 22, 6, 12, 9, 7, 12 },
            { 18172, 72000, 134.5, 7, 7, 9, 7, 6, 12, 5, 12 },
            { 18173, 72000, 144.4, 5, 18, 18, 22, 12, 15, 22, 22 },
            { 18174, 72000, 146.2, 12, 15, 9, 18, 27, 22, 12, 27 },
            { 18175, 72000, 133.6, 3, 4, 7, 7, 9, 5, 5, 4 },
            { 18176, 72000, 129.4, 6, 9, 6, 5, 3, 2, 5, 4 },
            { 18177, 72000, 151.2, 5, 15, 6, 7, 12, 2, 3, 6 },
            { 18178, 72000, 153.2, 3, 4, 5, 4, 3, 6, 12, 15 },
            { 18179, 72000, 160.5, 12, 32, 32, 39, 39, 67, 27, 15 },
            { 18180, 72000, 166.6, 32, 18, 39, 22, 18, 39, 18, 48 },
            { 18181, 72000, 183.6, 48, 48, 22, 48, 56, 56, 56, 32 },
            { 18182, 72000, 191, 39, 15, 27, 12, 22, 27, 27, 18 },
            { 18183, 72000, 199.8, 18, 32, 22, 22, 39, 48, 32, 32 },
            { 18184, 72000, 198.2, 48, 48, 32, 27, 22, 39, 18, 12 },
            { 18185, 72000, 189, 27, 39, 18, 22, 22, 39, 22, 15 },
            { 18186, 72000, 178, 27, 22, 39, 22, 32, 12, 5, 7 },
            { 18187, 72000, 172.7, 4, 5, 2, 6, 9, 6, 9, 7 },
            { 18188, 72000, 169.6, 15, 7, 12, 4, 2, 0, 2, 2 },
            { 18189, 72000, 158.8, 3, 4, 2, 4, 4, 3, 3, 3 },
            { 18190, 72000, 158.5, 18, 48, 7, 7, 6, 15, 18, 39 },
            { 18191, 72000, 160.3, 132, 179, 207, 56, 39, 32, 67, 18 },
            { 18192, 72000, 164.5, 27, 39, 18, 12, 56, 48, 32, 27 },
            { 18193, 72000, 158.8, 39, 27, 32, 22, 27, 15, 18, 32 },
            { 18194, 72000, 179.2, 22, 18, 15, 18, 22, 18, 9, 3 },
            { 18195, 72000, 189.4, 3, 9, 7, 12, 9, 12, 12, 12 },
            { 18196, 72000, 197.3, 12, 27, 12, 18, 15, 39, 15, 12 },
            { 18197, 72000, 183.9, 9, 6, 12, 6, 32, 27, 56, 18 },
            { 18198, 72000, 179.6, 12, 7, 7, 9, 7, 4, 12, 12 },
            { 18199, 72000, 169.4, 4, 4, 6, 7, 9, 7, 4, 6 },
            { 18200, 72000, 160.4, 7, 7, 7, 22, 7, 7, 6, 15 },
            { 18201, 72000, 150.6, 12, 9, 7, 22, 9, 6, 9, 5 },
            { 18202, 72000, 142.8, 12, 7, 7, 7, 4, 4, 5, 7 },
            { 18203, 72000, 143.1, 15, 2, 2, 5, 4, 2, 6, 7 },
            { 18204, 72000, 147.5, 7, 9, 4, 3, 4, 3, 3, 5 },
            { 18205, 72000, 160.5, 5, 3, 2, 2, 2, 3, 7, 15 },
            { 18206, 72000, 150, 15, 5, 6, 7, 6, 9, 22, 6 },
            { 18207, 72000, 173.9, 18, 32, 39, 32, 48, 48, 12, 18 },
            { 18208, 72000, 191.9, 27, 22, 39, 22, 39, 22, 32, 32 },
            { 18209, 72000, 229.9, 22, 39, 22, 27, 27, 32, 27, 32 },
            { 18210, 72000, 248.5, 18, 18, 9, 18, 15, 18, 22, 22 },
            { 18211, 72000, 239.8, 22, 39, 27, 27, 18, 18, 27, 27 },
            { 18212, 72000, 231.9, 15, 9, 9, 7, 7, 7, 15, 15 },
            { 18213, 72000, 223.8, 32, 32, 22, 12, 48, 48, 94, 56 },
            { 18214, 72000, 218.8, 12, 9, 12, 7, 12, 12, 15, 12 },
            { 18215, 72000, 205.6, 9, 3, 5, 5, 5, 3, 4, 4 },
            { 18216, 72000, 233.4, 4, 5, 7, 22, 22, 32, 12, 12 },
            { 18217, 72000, 221.3, 7, 6, 9, 15, 15, 15, 15, 12 },
            { 18218, 72000, 217.9, 15, 15, 18, 15, 15, 15, 22, 18 },
            { 18219, 72000, 210, 12, 18, 9, 6, 7, 6, 9, 27 },
            { 18220, 72000, 204.3, 18, 12, 9, 12, 4, 4, 4, 6 },
            { 18221, 72000, 210.1, 9, 12, 18, 7, 3, 12, 18, 15 },
            { 18222, 72000, 192, 22, 7, 5, 22, 18, 7, 5, 4 },
            { 18223, 72000, 185.6, 7, 27, 39, 12, 12, 9, 12, 32 },
            { 18224, 72000, 186.7, 18, 18, 27, 48, 15, 15, 15, 9 },
            { 18225, 72000, 183.7, 7, 9, 15, 39, 22, 15, 18, 15 },
            { 18226, 72000, 172.2, 5, 4, 4, 6, 7, 2, 0, 0 },
            { 18227, 72000, 169, 0, 0, 2, 2, 5, 2, 0, 3 },
            { 18228, 72000, 174.8, 7, 7, 9, 9, 6, 9, 7, 12 },
            { 18229, 72000, 163.9, 9, 5, 3, 6, 4, 0, 2, 5 },
            { 18230, 72000, 162.7, 9, 9, 7, 15, 18, 4, 9, 12 },
            { 18231, 72000, 165, 9, 3, 2, 5, 3, 5, 7, 5 },
            { 18232, 72000, 165.5, 6, 2, 2, 3, 6, 3, 6, 9 },
            { 18233, 72000, 151.8, 12, 12, 7, 27, 15, 22, 15, 15 },
            { 18234, 72000, 147.5, 32, 15, 27, 48, 32, 48, 22, 18 },
            { 18235, 72000, 142.7, 27, 22, 32, 22, 12, 22, 15, 12 },
            { 18236, 72000, 142.8, 9, 32, 22, 18, 22, 15, 15, 9 },
            { 18237, 72000, 153.3, 22, 12, 18, 18, 9, 15, 18, 15 },
            { 18238, 72000, 150.1, 15, 9, 22, 12, 9, 15, 12, 18 },
            { 18239, 72000, 156.2, 18, 22, 12, 12, 9, 7, 15, 15 },
            { 18240, 72000, 164.4, 7, 12, 7, 5, 7, 15, 18, 7 },
            { 18241, 72000, 159.1, 4, 9, 12, 7, 18, 7, 0, 0 },
            { 18242, 72000, 159.2, 5, 12, 3, 7, 5, 15, 22, 18 },
            { 18243, 72000, 166.1, 27, 39, 67, 27, 22, 18, 5, 2 },
            { 18244, 72000, 168.4, 5, 4, 3, 0, 0, 4, 5, 0 },
            { 18245, 72000, 178.7, 2, 2, 0, 0, 2, 2, 5, 15 },
            { 18246, 72000, 194, 6, 7, 7, 3, 3, 4, 5, 6 },
            { 18247, 72000, 200.7, 7, 12, 6, 5, 7, 9, 7, 3 },
            { 18248, 72000, 205.5, 4, 3, 5, 4, 4, 12, 9, 9 },
            { 18249, 72000, 206.9, 4, 7, 3, 4, 7, 5, 4, 4 },
            { 18250, 72000, 209.2, 0, 0, 3, 5, 2, 4, 3, 6 },
            { 18251, 72000, 217.2, 2, 2, 4, 3, 0, 3, 3, 2 },
            { 18252, 28800, this.data.getInstantFluxValue(this.format(18252, 28800)), 0, 0, 0, 0, 2, 2, 2, 0 } };
    }
}
