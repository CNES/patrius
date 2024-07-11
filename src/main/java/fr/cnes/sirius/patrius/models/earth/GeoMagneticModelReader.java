/**
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
 */
/* Copyright 2011-2012 Space Applications Services
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:92:17/05/2013:Class made abstract in order to fit the Orekit data loading mechanism
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Loads geomagnetic field models from a given input stream. A stream may contain multiple
 * models, the loader reads all available models in consecutive order.
 * <p>
 * The format of the expected model file is the following:
 * </p>
 * 
 * <pre>
 *     {model name} {epoch} {nMax} {nMaxSec} {nMax3} {validity start}
 *     {validity end} {minAlt} {maxAlt} {model name} {line number}
 * {n} {m} {gnm} {hnm} {dgnm} {dhnm} {model name} {line number}
 * </pre>
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 *    WMM2010  2010.00 12 12  0 2010.00 2015.00   -1.0  600.0          WMM2010   0
 * 1  0  -29496.6       0.0      11.6       0.0                        WMM2010   1
 * 1  1   -1586.3    4944.4      16.5     -25.9                        WMM2010   2
 * </pre>
 * 
 * @author Thomas Neidhart
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class GeoMagneticModelReader implements DataLoader, GeoMagneticDataProvider {
    // CHECKSTYLE: resume AbstractClassName check

    /** The loaded models. */
    private final List<GeoMagneticField> models = new LinkedList<GeoMagneticField>();

    /** Supported names. */
    private final String names;

    /**
     * Simple constructor.
     * <p>
     * Build an uninitialized reader.
     * </p>
     * 
     * @param supportedNames
     *        regular expression for supported files names
     */
    protected GeoMagneticModelReader(final String supportedNames) {
        this.names = supportedNames;
    }

    /**
     * Returns a {@link Collection} of the {@link GeoMagneticField} models that
     * have been successfully loaded. The {@link Collection} is in
     * insertion-order, thus it may not be sorted in order of the model epoch.
     * 
     * @return a {@link Collection} of {@link GeoMagneticField} models
     */
    @Override
    public Collection<GeoMagneticField> getModels() {
        return this.models;
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return this.models.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public abstract void loadData(InputStream input, String name) throws IOException, ParseException, PatriusException;

    /**
     * Get the regular expression for supported files names.
     * 
     * @return regular expression for supported files names
     */
    public String getSupportedNames() {
        return this.names;
    }

    /**
     * Add a {@link GeoMagneticField} to the models list.
     * 
     * @param model
     *        model to add to list
     */
    protected void add(final GeoMagneticField model) {
        synchronized (this.models) {
            this.models.add(model);
        }
    }
}
