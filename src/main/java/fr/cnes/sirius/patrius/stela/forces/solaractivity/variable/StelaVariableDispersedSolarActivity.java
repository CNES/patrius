package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.StelaSolarActivityType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

import java.util.TreeMap;

/**
 * Class for variable solar activity with a multiplicative coefficient applied.
 * 
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaVariableDispersedSolarActivity extends StelaVariableSolarActivity {

    /** Serializable UID. */
    private static final long serialVersionUID = -1650277535382842656L;

    /** Coefficient applied to the solar activity F10.7. */
    private final double f107Coef;

    /** Coefficient applied to the solar activity AP. */
    private final double apCoef;

    // =============================== CONSTRUCTORS ================================ //
    /**
     * Constructor of a variable solar activity with a multiplicative coefficient.
     * 
     * @param apCoef multiplicative coefficient for AP
     * @param f107Coef multiplicative coefficient for F10.7
     * @param reader the solar activity reader
     */
    public StelaVariableDispersedSolarActivity(final double apCoef, final double f107Coef,
                                               final SolarActivityDataProvider reader)
        throws PatriusException {
        super(reader, StelaSolarActivityType.VARIABLE_DISPERSED);
        this.apCoef = apCoef;
        this.f107Coef = f107Coef;
    }

    /**
     * Empty constructor. <br>
     * Default values are set: apCoef = 0 ; f107Coef = 0
     * 
     * @param reader the solar activity reader
     * 
     * @throws PatriusException for an initialization error
     */
    public StelaVariableDispersedSolarActivity(final SolarActivityDataProvider reader) throws PatriusException {
        this(0, 0, reader);
    }

    // =============================== METHODS ================================ //

    /** {@inheritDoc} */
    @Override
    public double getInstantFluxValue(final AbsoluteDate date) throws PatriusException {
        return super.getInstantFluxValue(date) * this.f107Coef;
    }

    /** {@inheritDoc} */
    @Override
    public double getAp(final AbsoluteDate date) throws PatriusException {
        return getSolarActivity(date)[2];
    }

    /** {@inheritDoc} */
    @Override
    public double[] getSolarActivity(final AbsoluteDate dateIn) throws PatriusException {
        final double[] tmp = super.getSolarActivity(dateIn);
        tmp[0] *= this.f107Coef;
        tmp[1] *= this.f107Coef;
        for (int i = 2; i < tmp.length; i++) {
            tmp[i] *= this.apCoef;
        }
        return tmp;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "[ Solar Activity ]" + System.lineSeparator() + " Solar Activity Type : " + this.getSolActType()
                + System.lineSeparator() + " F10.7 coefficient : " + this.f107Coef + System.lineSeparator()
                + " Ap coefficient : " + this.apCoef;
    }

    /** {@inheritDoc} */
    @Override
    public StelaVariableDispersedSolarActivity copy() throws PatriusException {
        return new StelaVariableDispersedSolarActivity(this.apCoef, this.f107Coef, this.reader);
    }

    /**
     * Get the AP coefficient of the variable solar activity.
     * 
     * @return the AP coefficient of the variable solar activity.
     */
    public double getApCoef() {
        return this.apCoef;
    }

    /**
     * Get the F10.7 coefficient of the variable solar activity.
     * 
     * @return the F10.7 coefficient of the variable solar activity.
     */
    public double getFluxCoef() {
        return this.f107Coef;
    }

    /** {@inheritDoc} */
    @Override
    public TreeMap<AbsoluteDate, Double> getEntireAPMap() {
        final TreeMap<AbsoluteDate, Double> map = new TreeMap<>(super.getEntireAPMap());
        map.replaceAll((date, apValue) -> map.get(date) * this.apCoef);
        return map;
    }
}
