const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date();
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection(`users/${ALICE_UID}/invoices`).get());
  await assertFails(db.collection(`users/${ALICE_UID}/products`).get());
  await assertFails(
    db.doc(`users/${ALICE_UID}/clients/c1`).set({
      userId: ALICE_UID,
      name: "Walk-in",
      phone: "9999999999",
      openingBalance: 0,
      isDeleted: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Cross-user access is denied across collections", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`users/${BOB_UID}/products/p1`).set({
      userId: BOB_UID,
      name: "Ashwagandha",
      isService: false,
      sellingPrice: 450,
      gstPercent: 12,
      stock: 20,
      isDeleted: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${BOB_UID}/products`).get());
  await assertFails(aliceDb.doc(`users/${BOB_UID}/products/p1`).get());
  await assertFails(
    aliceDb.doc(`users/${BOB_UID}/clients/c1`).set({
      userId: ALICE_UID,
      name: "Spoof",
      phone: "123",
      openingBalance: 0,
      isDeleted: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Authenticated owner can CRUD settings, products, clients, invoices, expenses, POs, appointments, branches, employees, recurring, warranties, coupons, online_orders", async () => {
  const db = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/config/main`).set({
      userId: ALICE_UID,
      businessName: "Ayurveda Wellness",
      invoicePrefix: "INV",
      nextInvoiceNumber: 1,
      invoiceTemplate: "modern",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/products/prod_1`).set({
      userId: ALICE_UID,
      name: "Triphala Churna",
      isService: false,
      sellingPrice: 280,
      gstPercent: 5,
      stock: 50,
      isDeleted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/clients/cli_1`).set({
      userId: ALICE_UID,
      name: "Aarav Sharma",
      phone: "9876543210",
      openingBalance: 0,
      isDeleted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/invoices/inv_1`).set({
      userId: ALICE_UID,
      invoiceNumber: "INV-0001",
      docType: "invoice",
      status: "final",
      paymentStatus: "paid",
      paymentMode: "upi",
      clientName: "Aarav Sharma",
      dateStr: "2026-11-12",
      grandTotal: 294,
      items: [{ name: "Triphala Churna", qty: 1, rate: 280, gst: 5 }],
      isDeleted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/expenses/exp_1`).set({
      userId: ALICE_UID,
      title: "Clinic Rent",
      category: "Rent",
      amount: 15000,
      paymentMode: "bank",
      dateStr: "2026-11-12",
      approvalStatus: "approved",
      isDeleted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/purchase_orders/po_1`).set({
      userId: ALICE_UID,
      poNumber: "PO-001",
      vendorName: "Himalaya Herbs",
      dateStr: "2026-11-12",
      status: "sent",
      items: [{ name: "Triphala", qty: 20, rate: 150 }],
      totalAmount: 3000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/appointments/apt_1`).set({
      userId: ALICE_UID,
      clientName: "Aarav Sharma",
      serviceName: "Nadi Pariksha",
      dateStr: "2026-11-13",
      timeStr: "10:30 AM",
      status: "scheduled",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/branches/br_1`).set({
      userId: ALICE_UID,
      name: "Main Branch",
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/employees/emp_1`).set({
      userId: ALICE_UID,
      name: "Priya Verma",
      role: "Cashier",
      isActive: true,
      permissions: ["dash_view", "inv_create"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/recurring/rec_1`).set({
      userId: ALICE_UID,
      clientName: "Aarav Sharma",
      frequency: "monthly",
      nextRunDate: "2026-12-12",
      items: [{ name: "Retainer", qty: 1, rate: 5000, gst: 18 }],
      totalAmount: 5900,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/warranties/war_1`).set({
      userId: ALICE_UID,
      type: "warranty",
      clientName: "Aarav Sharma",
      productName: "Steam Unit",
      startDate: "2026-11-12",
      months: 12,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/coupons/cp_1`).set({
      userId: ALICE_UID,
      code: "DIWALI10",
      discountPercent: 10,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    db.doc(`users/${ALICE_UID}/online_orders/ord_1`).set({
      userId: ALICE_UID,
      customerName: "Rohan",
      totalAmount: 560,
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(db.collection(`users/${ALICE_UID}/invoices`).get());
});

test("Shadow update with ghost field and invalid enum fail validation", async () => {
  const db = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();

  await assertFails(
    db.doc(`users/${ALICE_UID}/products/prod_bad`).set({
      userId: ALICE_UID,
      name: "Test",
      isService: false,
      sellingPrice: 100,
      gstPercent: 18,
      stock: 5,
      isDeleted: false,
      ghostField: "hack",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    db.doc(`users/${ALICE_UID}/invoices/inv_bad`).set({
      userId: ALICE_UID,
      invoiceNumber: "INV-2",
      docType: "invalid_type",
      status: "final",
      paymentStatus: "paid",
      paymentMode: "upi",
      clientName: "Test",
      dateStr: "2026-11-12",
      grandTotal: 100,
      items: [],
      isDeleted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
