# Terrassorkesterns notgenerator 3.0

Detta program genererar noter i PDF format från scannat material. Programmet är skrivet i Java + Spring Boot och körs i
molnet! Du hittar det på [notgen.terrassorkestern.se](https://notgen.terrassorkestern.se).

De scannade noterna ligger lagrade i antingen ZIP eller PDF-filer. Vanligast och standard numera är ZIP. Dessa
bildbehandlas sedan för att förstora upp dem till A4 format för ökad läsbarhet.

Arbetsgången är följande:

1. Läs in info från databasen
2. Läs in arkivfil (zip eller pdf) från storage (lokalt eller Azure)
3. Packa upp i en temporär katalog
4. Bildbehandla de enskilda filerna
5. Skapa en PDF för varje låt och stämma
6. Spara omslaget (om det finns) lokalt

## Bildbehandling

Huvuddelen av noterna är i ett mindre format än A4 och när de scannas så blir det en tom marginal längst ner och till
höger (det är viktigt att man scannar med övre vänstre hörnet linjerat). Scanning skall också ske i färg och med en hög
upplösning (300 DPI).

Bildbehandlingen utgörs sedan av följande steg:

1. Rotera bilden vid behov
2. Beskär bilden så att marginalerna försvinner
3. Förstora den beskurna bilden till A4
4. Gör om från färg till gråskala
5. Gör om från gråskala till svartvitt (med Otsus algoritm, https://en.wikipedia.org/wiki/Otsu%27s_method)
6. Spara ner som PNG.

För omslag så sker det ingen färgkonvertering samt att filen sparas som JPG istället.

 ## Hantering av metadata

Vidare så finns det stöd för att hantera all metadata kring noterna. Detta innefattar upplägg och redigering av:

* Noter (låtar)
* Instrument
* Sättning (dvs. vilka instrument som ingår i vilka låtar)
* Låtlistor
* Användare
