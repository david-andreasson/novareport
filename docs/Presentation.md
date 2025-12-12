
# Nova Report – presentationsmanus
---

## Slide 1 – Titel: "Nova Report"

"Hej, jag heter David Andreasson och det här är mitt examensarbete: **Nova Report**.

En nyhetsaggregator för kryptvaluta-nyheter"


"Jag kommer först att gå igenom bakgrund, mål och arkitektur – med fokus på **separation of concerns** och på två nya tekniker jag aldrig jobbat med tidigare: **kryptovaluta‑betalningar** och **Discord‑integration**. Efter det kör jag en demo där jag tar en användare hela vägen från registrering till färdiga rapporter."

---

## Slide 2 – Agenda

"Här är en snabb översikt av vad jag tänker gå igenom:

- Först bakgrund och problem – varför jag byggde just det här.
- Sen mina mål och krav för examensarbetet.
- Efter det en arkitekturöversikt i tre versioner, som visar hur designen utvecklats.
- Jag presenterar kort mitt team.
- Sedan milstolparna UVMP, MVP, MMP och nice‑to‑have.
- En översikt av arbetsflödet och driften.
- Och sist innan demon: mer detaljer om ny teknik – framför allt Monero‑betalningen och Discord‑integrationen."

---

## Slide 3 – Bakgrund & problem

"Vi börjar med bakgrunden.

Problemet jag utgår ifrån är att det finns väldigt mycket brus i krypto‑världen. Det är nyheter, tweets, prisrörelser, rykten – och det är svårt att få en snabb och någorlunda neutral överblick.

Min idé med Nova Report är att låta en tjänst göra det här åt användaren:

- hämta in kryptonyheter från flera källor,
- låta en AI sammanfatta det viktigaste,
- och leverera en färdig, läsbar rapport med jämna intervall.

För mig personligen var det här en chans att träna på just sådant som jag tycker är intressant: mikrotjänster, integrationer mot externa system och lite mer avancerade betalflöden."

---

## Slide 4 – Mål & krav

"Utifrån det formulerade jag två typer av mål: övergripande mål och mer tekniska mål.

Övergripande mål var att bygga en plattform där en användare kan:

- skapa konto,
- köpa en prenumeration,
- och få återkommande rapporter levererade automatiskt.

På den tekniska sidan satte jag tre huvudmål:

- För det första: tydlig separation of concerns mellan tjänsterna. Konton, betalningar, prenumerationer, rapportgenerering och notifieringar skulle ligga i separata mikrotjänster.
- För det andra: implementera betalning med kryptovaluta, men på ett sätt där jag enkelt kan lägga till fler betalmetoder – till exempel Stripe.
- För det tredje: kunna skicka rapporter via e‑post och Discord, så att användaren inte måste logga in på sajten för att få värde av sin prenumeration.

Resten av presentationen handlar i princip om hur jag försökt uppnå de här målen."

---

## Slide 5 – Arkitekturöversikt, Version 1

Här ser ni den första versionen av arkitekturen jag skissade upp första veckan.

Flera nyhetskällor in, en reporter‑tjänst i mitten som hämtar all information, och flera kanaler ut – frontend, e‑post, Discord osv.

Redan här syns separation of concerns:

- `accounts-service` sköter bara registrering och inloggning.
- `subscriptions-service` håller koll på vem som har en aktiv prenumeration.
- `payments-xmr-service` sköter betalningar i Monero.
- `reporter-service` hämtar in data och bygger rapporten.
- `notifications-service` ansvarar för att distribuera rapporterna – inte för att skapa dem.

Redan här insåg jag att jag inte skulle hinna göra allt det här själv, utan hjälp. Vi har ju alla i klassen gjort såna här mikrotjänster förut och vet hur många olika klasser och metoder bara en av dessa innehåller. Så jag var tvungen att skaffa ett team, men det berättar jag mer om strax.

---

## Slide 6 – Arkitekturöversikt, Version 2

I vecka 2 hade jag kommit lite längre med min skiss.

Här ser vi samma grundstruktur, men jag har nu delat upp det i fyra steg istället för bara tre. Där första målet var en UVMP, en ultra minimum viabla product. En enda nyhetskälla in och ett enda informationsflöde ut. Mockad prenumeration, mockad betalning och mockad ai-sammanfattning. Bara för att kunna se informationsflödet fungera. 

---

## Slide 7 – Arkitekturöversikt, Version 3

"Version 3 är i princip det läge systemet är i idag.

Här har jag lyft upp alla mikrotjänster i en rad för att det ska bli enklare att se, inte för att se hur dom pratar med varandra, för skulle jag göra det så skulle det se ut som ett stort garn-nystan på skärmen. Utan bara för att visa hur det är uppdelat. Längst ner ser vi infrastrukturen: containers för frontend, varje tjänst, Postgres‑databaserna, Monero‑walleten och Stripe.

Överst till höger ser vi också indata – olika RSS‑flöden och API:er. Och längst ut till höger ser vi utdata – frontend, e‑post och Discord.

Det viktiga budskapet med den här bilden är två saker:

1. **Separation of concerns på tjänstenivå.** Varje ruta har ett tydligt ansvar. Vill jag lägga till en ny betalmetod kan jag göra det i en ny tjänst utan att röra accounts, reporter eller notifications.  

2. **Externa beroenden är kapslade.** Bara Monero‑betalningstjänsten pratar med Monero‑walleten. Bara notifications‑tjänsten pratar med mailservern och Discord. Det gör systemet både säkrare och enklare att felsöka."

---

## Slide 8 – Mitt team

"Jag gjorde inte det här helt ensam – jag hade ett litet ‘team’ runt mig.

- **David Andreasson**, Det är ju jag. Jag har fungerat lite som en puppet master där jag dragit i trådar för att få dom olika ai-tjänsterna att göra som jag vill..
- **Windsurf IDE med Cascade‑AI** – den IDE jag har använt – i den har jag använt Cascade-tjänsten (deras integrerade ai) som diskussionspartner kring arkitektur, designbeslut och felsökning. MYCKET felsökning.

- **GitHub Copilot** har hjälpt till som ‘senior utvecklare / PR‑granskare’ – föreslagit kod genom kommenterar på mina pull requests.

- Och **ChatGPT** har jag använt för att dels modifiera befintlig kod jag redan hade, som till exempel ett user-service-skelett jag använt från tidigare projekt fick bli min början på accounts-service. Men också för att skriva audit-prompter som jag sedan använt i Windsurf för att göra kod-audits men jämna mellanrum.

Det är tillsammans med de här verktygen som gjort att jag kunnat slutföra ett sånt här pass komplicerat och ganska stort projekt på ensam hand. 

---

## Slide 9 – Mina milstolpar

"Jag delade upp projektet i fyra nivåer av färdighet: **UVMP, MVP, MMP och NTH**.

- **UVMP** – Ultra Viable Minimum Product – var att få upp en **minimal end‑to‑end‑lösning**: hämta kryptonyheter från endast en källa och visa en enkel rapport, initialt med fejkad AI‑sammanfattning, utan betalningar eller prenumerationer.

- **MVP** – här skulle användaren få ett komplett flöde: registrera konto, köpa prenumeration med Monero och sedan kunna se rapporter.

- **MMP** – Minimum Marketable Product – handlade om att göra tjänsten mer produktionsredo: Det ska fungera att skapa ett konto, genomföra en betalning, trevligare frontend, mcyket logging och övervakning, och leverans av rapport via frontend, email och Discord.

- **Nice to have** – om tid fanns – var att lägga till extra betalmetoder som Stripe, fler kanaler och andra experimentella funktioner.

---

## Slide 10 – Arbetsflöde

"Den här bilden visar hur mitt dagliga arbetsflöde såg ut.

Jag började nästan alltid vid datorn med att formulera vad jag ville uppnå: till exempel ‘lägg till Discord‑notiser’ eller ‘gör Monero‑betalningen mer robust’. Sen jobbade jag i Git, ofta i små grenar, med stöd av Copilot och Cascade.

När koden var klar pushade jag till GitHub, byggde Docker‑images och rullade ut det på min **Ubuntu‑server** med Portainer och Nginx som front.

Poängen här är att jag försökt efterlikna ett **riktigt utvecklingsflöde**: versionshantering, containerisering och en tydlig väg från kod till körande tjänst. Det gjorde det också enklare att testa hela systemet lokalt innan jag visade något för handledaren."

---

## Slide 11 – Ny teknik i projektet (översikt)

"Om vi zoomar in på ny teknik jag jobbat med i det här projektet så är det framför allt:

- **Kryptovaluta‑betalningar**, där jag valt valutan Monero, dess förkortning är XMR.
- **Discord‑integration** för att skicka leverera rapporter direkt in i en kanal.
- **Postgres‑databas** i stället för bara H2 och MySQL.
- **E‑post i Spring** via notifications‑tjänsten.

Jag kommer strax att fördjupa mig i Monero‑betalningsflödet och Discord‑integrationens design, eftersom de är mest intressanta."

---

## Slide 12 – Betalningsflöde XMR

"Nu går vi in på Monero‑betalningen, men fortfarande på en nivå som ska vara begriplig utan att kunna koden.

Vi kan tänka oss tre steg:

**1. Skapa betalning**  
Användaren klickar ‘Prenumerera’ i webbgränssnittet och väljer en plan. Frontenden anropar då en renodlad betalningstjänst, `payments-xmr-service`. Den tjänsten skapar en ny betalning i databasen, genererar en unik Monero‑adress och ett belopp, och skickar tillbaka det till frontenden, som visar adress och QR‑kod.

**2. Bevaka Monero‑plånboken**  
I bakgrunden kör en separat process som en gång i minuten, tittar i Monero‑plånboken: har det kommit in tillräckligt mycket XMR på just den här adressen? Här använder jag pollning därför att Monero‑noden är ett separat system som inte kan skicka events tillbaka till mig på ett enkelt sätt.

**3. Aktivera prenumeration**  
När rätt belopp har kommit in markeras betalningen som betald. Då skickas en **intern signal** vidare till en annan komponent som aktiverar prenumerationen i `subscriptions-service` och ber `notifications-service` skicka ett bekräftelsemail.

Här syns **separation of concerns** väldigt tydligt:

- Web‑frontenden vet bara att den ska visa betalningsinformation och senare en framgångs‑ruta.
- `payments-xmr-service` håller ordning på betalningsstatus och pratar med Monero‑walleten.
- `subscriptions-service` bestämmer vem som faktiskt har tillgång till rapporterna.
- `notifications-service` sköter kommunikationen med användaren.

Om till exempel Monero‑delen skulle ligga nere kan jag fortfarande ta betalt med kort via Stripe, eftersom den ligger i en helt separat tjänst men ändå använder samma prenumerations‑ och notifieringsflöde."

---

## Slide 13 – Discord‑integration

"Den andra delen jag vill lyfta fram är **Discord‑integrationen**.

Här handlar det om hur färdiga rapporter tar sig hela vägen till en Discord‑kanal.

**Steg 1 – Systemet genererar rapport**  
`reporter-service` hämtar in kryptonyheter från flera RSS‑flöden och API:er, låter AI‑tjänsten 1min.ai sammanfatta innehållet, och sparar en färdig rapport.

**Steg 2 – Skickar till notistjänsten**  
När en ny rapport är klar anropar reporter‑tjänsten `notifications-service` via ett internt API och säger i princip: ‘Här är dagens rapport, gör den tillgänglig för användarna.’

**Steg 3 – Publicerar i Discord**  
I notifications‑tjänsten finns en del som är specialiserad på Discord. Den formaterar rapporten till Discord‑vänliga **embeds**, tar hänsyn till begränsningar i antal tecken och antal fält, och skickar sedan JSON‑payloaden till en säker webhook‑URL för den kanal jag valt.

Även här är rollerna tydligt uppdelade:

- Reporter‑tjänsten bryr sig bara om **innehållet** – vad som ska stå i rapporten.
- Notifications‑tjänsten bryr sig om **leveransen** – hur vi pratar med Discords API och hur vi hanterar fel.
- Frontenden till exempel, den behöver inte veta något om Discord‑formatet. Den läser bara ‘senaste rapporten’ från en enkel endpoint och visar den i webbläsaren.

Det betyder att om jag i framtiden vill lägga till till exempel Telegram eller någon annan kanal, kan jag göra det i notifications‑tjänsten utan att ändra hur rapporter genereras."

---

## Övergång till demo

"Nu har jag pratat mycket om arkitektur, separation of concerns och hur Monero‑betalningarna och Discord‑integrationens flöden ser ut i teorin.

Nästa steg är att visa hur det faktiskt ser ut i praktiken.

I demon kommer jag att ta en användare hela vägen:

1. Skapa konto och logga in.
2. Köpa en prenumeration – först med Monero, och beroende på tid också med Stripe.
3. Visa hur rapporten dyker upp i webbgränssnittet.
4. Och till sist hur rapporten levereras via e‑post och i Discord‑kanalen.

Under tiden kommer jag att peka tillbaka på några av de här arkitektur‑bilderna för att koppla ihop det ni ser i UI:t med de mikrotjänster som snurrar i bakgrunden."

"Med det sagt går vi över till demon."

---
