# Terrassorkesterns notgenerator 2.0

Detta program genererar noter i PDF format från scannat material. Programmet är skrivet i Java + Spring Boot och kräver tillgång till orkesterns databas samt till den filserver där noterna ligger.

Noterna ligger lagrade i antingen ZIP,  eller PDF-filer. Vanligast och standard numera är ZIP.

Arbetsgången är följande:
1. Läs in info från databasen
2. Läs in arkivfil (zip eller pdf)
3. Packa upp i en temporär katalog
4. Bildbehandla de enskilda filerna
5. Skapa en ny PDF med omslag och alla stämmor och ladda upp till Google Drive.
6. Skapa en ny PDF med bara TO stämmor och ladda upp till Google Drive.
7. Skapa en PDF per instrument och ladda upp till Google Drive.
8. Ladda upp omslaget (om det finns) till Google Drive.

## Bildbehandling
Huvuddelen av noterna är i ett mindre format än A4 och när de scannas så blir det en tom marginal längst ner och till höger (det är viktigt att man scannar med övre vänstre hörnet linjerat). Scanning skall också ske i färg och med en hög upplösning (300 DPI).

Bildbehandlingen utgörs sedan av följande steg:
1. Rotera bilden vid behov
2. Beskär bilden så att marginalerna försvinner
3. Förstora den beskurna bilden till A4
4. Gör om från färg till gråskala
5. Gör om från gråskala till svartvitt (med Otsus algoritm, https://en.wikipedia.org/wiki/Otsu%27s_method)
6. Spara ner som PNG.

För omslag så sker det ingen färgkonvertering samt att filen sparas som JPG istället.

## Listning
Det finns också en ytterligare funktion som skapar ett Google Sheet dokument med en lista på alla noter och sättning.

## Hantering av metadata
Vidare så finns det stöd för att hantera all metadata kring noterna. Detta innefattar upplägg och redigering av:
* Noter (låtar)
* Instrument
* Sättning (dvs. vilka instrument som ingår i vilka låtar)
* Låtlistor
* Användare
