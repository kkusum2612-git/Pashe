
# Medicine Care and Health Passport

A Health Passport adapter can implement `HealthPassportAllergyReader` as a Spring bean. For the supplied patient UUID, return the currently recorded allergies with stable allergy UUIDs, the catalog's drug-class name for a structured allergy, and any free text. The contract represents an available passport with no allergies as `Optional.of(List.of())` and an unavailable passport as `Optional.empty()`. Medicine Care displays the latter state while ingredient duplication checks continue.

The safety API recalculates warnings from the current allergy snapshot and medication list on each read. A Health Passport change is visible on the next request to `GET /api/v1/medicine/safety`. My Medicines refreshes the report when the page regains focus, when the patient requests a refresh, and after a medicine change. If Health Passport adds an in-app editing view, refresh this report after the allergy write commits. Allergy records stay in Health Passport. My Medicines remains the source for the medication list through `UserMedicineService.getMyMedicines(patientId)`, with authorization handled by the calling feature.

Related-class warnings use the curated `medicine_related_drug_classes` table. The initial penicillin–cephalosporin example is a class-level advisory. Clinical review of the catalog classes and curated pairs is pending. [CDC guidance on reported penicillin allergy](https://www.cdc.gov/std/treatment-guidelines/penicillin-allergy.htm)

Prescription access uses authenticated, owner-scoped endpoints with private, no-store responses. Care Circle and pharmacy access depend on their respective consent and active-request authorization models. Add those access checks before issuing any delegated image links.
