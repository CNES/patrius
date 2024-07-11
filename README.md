![patrius_logo](http://patrius.cnes.fr/resources/assets/wiki.png "Patrius logo") 


# PATRIUS Source Code

This project contains the source code of the PATRIUS library. This code corresponds to **Patrius v4.10.4**.

## ABOUT

PATRIUS is a core space dynamics Java library developed by the [CNES](http://cnes.fr), the French Space Agency, that enables developers to quickly develop high level algorithms such as orbit extrapolator. PATRIUS contains several sub-libraries that work together and cover low level classes (i.e.: such as matrix, vectors, orbits parameters) as well as high level classes and interfaces (i.e.: numerical propagators, attitude laws, manoeuvers sequences). 

All the main domains of space dynamics are available: 

  - Analysis, algebra and geometry core library (quaternions, derivable functions, integrators …) 
  - Core objects for space dynamics (dates, orbits, frames...) 
  - Orbit propagation: analytical, semi-analytical and numerical propagators, a full set of force models 
  - Maneuvers: impulsive or continuous thrust, sequences 
  - Attitude: extensible set of attitude laws, sequences and guidance framework 
  - Events: event detection (orbital, sensor events, etc.) and post-processing (chronograms) 
  - Spacecraft: characteristics of mass, geometry (drag force), sensors field of view, etc. 

PATRIUS got a deep and thourough validation by comparing its results with that of other precise orbitography tools. 
Additionally, it has been flight-proven since 2018 and is used in plenty of CNES products, such as our operational flight dynamics subsystems (FDS). For that reason the ECSS criticality is categorized as “C”.

PATRIUS' design relies on extensible Java interfaces and robust design patterns.


## PROJECT DOCUMENTATION

PATRIUS has its own Wiki accessible at the following address: http://patrius.cnes.fr
Through this link, you can also access the project overview, architecture and development, detailed features list, Javadoc and a lot more information.


## RELEASES

Official releases are available on https://www.connectbycnes.fr/patrius.


## SUPPORT

patrius@cnes.fr


## LICENCE

PATRIUS is under Apache Licence 2.0.
