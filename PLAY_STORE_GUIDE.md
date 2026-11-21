# Ανέβασμα του Deepfall Drill στο Google Play — βήμα προς βήμα

Τελευταία ενημέρωση: με βάση το build **1.2.0 (versionCode 3)**.

## Τι είναι ήδη έτοιμο

| Πράγμα | Κατάσταση |
| --- | --- |
| Package | `com.deepfalldrill.game` (μόνιμο μετά το πρώτο upload) |
| Version | `1.2.0`, versionCode `3` |
| Signed AAB (1.2.0, ready for internal testing) | `app\build\outputs\bundle\release\app-release.aab` |
| APK (μόνο για τοπικό test) | `app\build\outputs\apk\release\app-release.apk` |
| Υπογραφή | Ναι, upload key `CN=Deepfall Drill` |
| R8 / shrink | Ναι (release) |
| targetSdk / minSdk | 36 / 24 |
| Άδειες | Μόνο `VIBRATE` (χωρίς internet) |
| Store assets | `store\` (icon 512, feature 1024×500, 4 screenshots 1080×1920) |
| Listing copy | `store\listing.md` (updated for 1.2.0) |
| Privacy policy | `PRIVACY.md` (θέλει email + hosting) |
| Mapping file | `app\build\outputs\mapping\release\mapping.txt` |
| Packaged upload kit | `dist\deepfall-drill-play-upload-1.2.0.zip` |

Πριν πατήσεις το πρώτο upload, απόφαση που δεν αλλάζει μετά:

- **Package name**: αν θέλεις κάτι άλλο από `com.deepfalldrill.game` (π.χ.
  `com.deepfalldrill`), πες το **τώρα**. Μετά το πρώτο upload δεν αλλάζει.

---

## Βήμα 1 — Developer account (μία φορά)

1. Πήγαινε στο https://play.google.com/console και συνδέσου με Google account.
2. Πληρώνεις **$25** μία φορά.
3. Συμπλήρωσε **Developer profile** (όνομα, διεύθυνση, τηλέφωνο) και κάνε
   **identity verification** (ταυτότητα/διεύθυνση). Μπορεί να πάρει 1–2 μέρες.
4. Ενεργοποίησε **2-Step Verification** στον Google account.
5. Το developer name και το email θα είναι **δημόσια** στη σελίδα του app. Βάλε
   ένα email που ελέγχεις (π.χ. `support@...`).

> Αν ο λογαριασμός είναι **personal** και δημιουργήθηκε μετά τις 13/11/2023,
> για να βγεις σε production πρέπει πρώτα να κάνεις **closed testing με 12+
> testers για 14 συνεχόμενες μέρες**. Οι organization accounts μπορεί να
> εξαιρούνται — δες το Dashboard του λογαριασμού σου.

---

## Βήμα 2 — Κράτα ασφαλή το upload key (μία φορά, ΚΡΙΣΙΜΟ)

Τα αρχεία:

```
upload-keystore.jks
keystore.properties   (περιέχει το password)
```

- Αντέγραψέ τα σε password manager / κρυπτογραφημένο δίσκο / USB.
- Μην τα ανεβάσεις σε git. Είναι ήδη στο `.gitignore`.
- Αν χαθεί το key: Play Console → App signing → **Request upload key reset**
  (όχι αυτόματο). Το app signing key το κρατά η Google.

---

## Βήμα 3 — Privacy policy (πριν το upload)

Το `PRIVACY.md` είναι έτοιμο περιεχόμενο, αλλά:

1. Άνοιξέ το και αντικατέστησε το `YOUR_EMAIL_HERE` με πραγματικό email.
2. Ανέβασέ το δημόσια. Ο πιο απλός τρόπος, GitHub Pages:
   - Φτιάξε δημόσιο repo, π.χ. `deepfall-drill-privacy`.
   - Χρησιμοποίησε το έτοιμο `store/privacy-policy.html` (αντίγραψέ το ως
     `index.html` στο repo), αφού αντικαταστήσεις το `YOUR_EMAIL_HERE`.
   - Settings → **Pages** → Deploy from a branch → `main` / `(root)` → Save.
   - Θα πάρεις URL τύπου `https://<user>.github.io/deepfall-drill-privacy/`.
3. Βάλε αυτό το URL στο Play Console → **Policy → App content → Privacy policy**.
4. Εναλλακτικά: Google Sites, Notion public page, ή δικός σου server.

Το παιχνίδι **δεν** έχει internet permission, δεν έχει ads/analytics SDKs και
αποθηκεύει progress μόνο τοπικά. Άρα το Data safety είναι απλό.

---

## Βήμα 4 — Δημιουργία app στο Play Console

1. **All apps → Create app**.
2. App name: `Deepfall Drill`
3. Default language: English (ή Greek — ό,τι θέλεις).
4. App or game: **Game**. Free or paid: **Free**.
5. Αποδέξου τις δηλώσεις.

Μετά άνοιξε το **Dashboard** και ολοκλήρωσε τα «Set up your app» tasks με τη
σειρά. Απαντήσεις:

| Πεδίο | Απάντηση |
| --- | --- |
| App access | **All functionality is available without special access** |
| Ads | **No ads** |
| In-app purchases | **No** |
| Content rating | **Start questionnaire** (οδηγίες παρακάτω) |
| Target audience | **13+** (προτείνεται — αν διαλέξεις κάτω από 13 μπαίνεις σε Families policy) |
| News app | No |
| COVID-19 apps | No |
| Government apps | No |
| Financial features | No |
| Health | No |
| Privacy policy | Το URL από το Βήμα 3 |
| Data safety | Progress is local; disclose user-triggered run sharing as applicable |

### Content rating questionnaire (σύντομα)

- Category: **Game**
- Violence: **Yes** — cartoon/fantasy drilling και μικρές εκρήξεις, χωρίς αίμα.
- Blood: No
- Sexual content: No
- Bad language: No
- Controlled substances: No
- Gambling: No
- User interaction / UGC / chat: No
- Shares location: No
- Purchases: No
- Ads: No

Αποτέλεσμα συνήθως **Everyone 10+ / PEGI 7**. Απάντησε ειλικρινά· το σύστημα
βγάζει το rating.

### Data safety

- The game has no account, backend, ads or analytics and collects no personal data.
- The player can explicitly send generated run details (depth, seed code, death line)
  to another app through Android's Share sheet. Describe this accurately in the
  Play data-safety form if the current form classifies that user-initiated transfer
  as shared data; do not claim that nothing can be shared.
- Is all user data encrypted in transit? → N/A for game data (no game server)
- Do you provide a way to request data deletion? → N/A (no account)
- Personal data types collected by the game: none
- Ads/analytics SDKs: κανένα
- Το `VIBRATE` δεν είναι data collection.

---

## Βήμα 5 — Store listing

Στο **Grow → Store presence → Main store listing**:

- App name: `Deepfall Drill`
- Short description (≤80): από `store/listing.md`
- Full description (≤4000): από `store/listing.md`
- App icon: `store/icon-512.png` (512×512)
- Feature graphic: `store/feature-graphic-1024x500.png`
- Phone screenshots: current 1.2.0 captures are in `store/screenshots/`
  (portrait, genuine in-app screens; inspect once before upload)
- Category: **Games → Arcade**
- Contact email: το support email σου
- Privacy policy URL: από το Βήμα 3

Προαιρετικά: tablet screenshots, promo video, translations.

---

## Βήμα 6 — Internal testing (πρώτο upload)

1. **Testing → Internal testing → Create new release**.
2. Upload **`app/build/outputs/bundle/release/app-release.aab`** (ΟΧΙ το APK).
3. Στο πρώτο upload το Play θα σου προτείνει **Play App Signing** — αποδέξου.
   Το AAB είναι ήδη signed με το upload key· το Play θα το ξαναϋπογράψει με το
   app signing key που διαχειρίζεται.
4. Release notes: από `store/listing.md` (ή σύντομο κείμενο).
5. **Save → Review release → Start rollout**.
6. **Testers**: πρόσθεσε emails, αντίγραψε το opt-in link, εγκατέστησε σε
   πραγματικό κινητό.
7. Έλεγξε: ήχος, tilt/drag/zones, boost, αγορές, save/checkpoint, θάνατος.

Προαιρετικά: **Internal app sharing** για γρήγορο install χωρίς release.
Επίσης ενεργοποίησε **Pre-launch report** για αυτόματα tests σε συσκευές.

---

## Βήμα 7 — Closed testing (12 testers × 14 μέρες)

Ισχύει για **personal** developer accounts (νέοι λογαριασμοί). Αν έχεις
organization account, δες το Dashboard — μπορεί να μην απαιτείται.

1. **Testing → Closed testing → Create track** (π.χ. `beta`).
2. Δημιούργησε release με το **ίδιο AAB** (μπορείς να το προωθήσεις από το
   internal).
3. Πρόσθεσε **12+ testers** (emails ή Google Group) και στείλε το opt-in link.
4. Οι testers πρέπει να κάνουν opt-in και να κρατήσουν το app εγκατεστημένο για
   **14 συνεχόμενες μέρες**.
5. Μετά: **Dashboard → Apply for production access** και απάντησε στις ερωτήσεις
   (πώς έγινε το testing, τι feedback πήρες, τι διορθώθηκε).

Μην προσπαθήσεις να παρακάμψεις το βήμα· το production κλειδώνει.

---

## Βήμα 8 — Production

1. **Production → Create new release**.
2. Upload/promote το AAB.
3. **Release notes** (copy από `store/listing.md`).
4. **Countries/regions**: διάλεξε όλες ή τις χώρες σου.
5. **Rollout**: ξεκίνα με **20%** (staged rollout) αν θέλεις ασφάλεια.
6. **Save → Review release → Start rollout**.
7. Το review συνήθως παίρνει ώρες έως λίγες μέρες. Έλεγχε το email σου.

Μετά την έγκριση:

- **Quality → Android vitals**: crashes/ANRs.
- Απάντησε σε reviews.
- Ανέβασε το **`mapping.txt`** στο release (Deobfuscation file) για readable
  crash reports.
- Για updates: αύξησε **versionCode** (3, 4, …) και φτιάξε νέο AAB.

---

## Troubleshooting (συχνές απορρίψεις)

| Πρόβλημα | Λύση |
| --- | --- |
| Privacy policy missing/inaccessible | Δημόσιο URL, χωρίς login, σωστό email |
| Data safety mismatch | Περιέγραψε σωστά το Share sheet και δήλωσε μόνο όσα ισχύουν για την έκδοση |
| App not targeting recent Android | targetSdk 36 — ok |
| Personal account production blocked | 12 testers × 14 μέρες closed testing |
| Upload key error | Χρησιμοποίησε το ίδιο `upload-keystore.jks` / alias `deepfall` |
| «App bundle was signed with the wrong key» | Το AAB είναι signed με το upload key· καμία αλλαγή |
| Screenshots rejected | 1080×1920, 4 τεμάχια, χωρίς πλαίσια/κείμενο |
| Misleading metadata | Μην αναφέρεις features που δεν υπάρχουν |

---

## Checklist πριν το πρώτο upload

- [ ] `PRIVACY.md`: αντικατάσταση `YOUR_EMAIL_HERE` + δημόσιο URL
- [ ] Επιβεβαίωση package name `com.deepfalldrill.game` (ή αλλαγή **τώρα**)
- [ ] Backup `upload-keystore.jks` + `keystore.properties`
- [ ] `store/listing.md` έτοιμο
- [ ] Icon 512 + feature graphic + 4 genuine screenshots from 1.2.0
- [ ] Content rating, Data safety, App access, Target audience
- [ ] Internal testing install σε πραγματικό κινητό
- [ ] Closed testing με 12 testers (αν personal account)
- [ ] `versionCode` 3 για αυτό το upload
- [ ] Mapping file φυλαγμένο

---

## Χρήσιμα paths

```
app/build/outputs/bundle/release/app-release.aab   → upload στο Play
app/build/outputs/apk/release/app-release.apk     → μόνο sideload test
app/build/outputs/mapping/release/mapping.txt     → crash deobfuscation
store/                                             → listing assets/copy
PRIVACY.md                                         → hosting
```
