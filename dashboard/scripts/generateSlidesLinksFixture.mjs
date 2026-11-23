/*
Copyright 2026 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import PDFDocument from 'pdfkit';
import { createWriteStream } from 'node:fs';

// Run from the dashboard with: node scripts/generateSlidesLinksFixture.mjs
const doc = new PDFDocument({ size: [600, 400], margin: 40 });
doc.pipe(createWriteStream(new URL('../../e2e-tests/cypress/fixtures/slides-link-actions.pdf', import.meta.url)));
const optionalLayer = doc.ref({ Type: 'OCG', Name: new String('Review layer') });
optionalLayer.end();
doc._root.data.OCProperties = {
    OCGs: [optionalLayer],
    D: { Order: [optionalLayer], ON: [optionalLayer], BaseState: 'ON' },
};
for (let page = 1; page <= 3; page += 1) {
    if (page > 1) doc.addPage();
    doc.fontSize(20).text(`Link actions slide ${page}`, 40, 40);
    // Fixed rectangles allow the UI tests to assert both position and size.
    doc.rect(40, 100, 180, 24).stroke();
    doc.fontSize(12).text('Absolute HTTPS link', 44, 104);
    doc.link(40, 100, 180, 24, 'https://skilltreeplatform.dev');
    doc.text('Relative documentation link', 44, 144);
    doc.link(40, 140, 180, 24, 'docs/intro.html');
    ['NextPage', 'PrevPage', 'FirstPage', 'LastPage'].forEach((action, index) => {
        const x = 40 + index * 130;
        doc.text(action, x, 190);
        doc.annotate(x, 185, 120, 24, { Subtype: 'Link', A: { S: 'Named', N: action } });
    });
    ['javascript:alert(1)', 'data:text/html,unsafe', 'file:///etc/passwd'].forEach((uri, index) => {
        doc.text(`Blocked URI ${index + 1}`, 40, 240 + index * 30);
        doc.link(40, 235 + index * 30, 180, 24, uri);
    });
    doc.annotate(300, 235, 180, 24, {
        Subtype: 'Link',
        // PDFKit serializes primitive strings as PDF names in raw dictionaries.
        // Boxed strings produce the PDF string payload required by these actions.
        A: { S: 'JavaScript', JS: new String('globalThis.pdfScriptExecuted = true; app.alert("unsafe")') },
    });
    doc.annotate(300, 275, 180, 24, { Subtype: 'Link', A: { S: 'Launch', F: new String('file:///etc/passwd') } });
    doc.text('Unsupported layer toggle', 300, 315);
    doc.annotate(300, 310, 180, 24, {
        Subtype: 'Link',
        A: { S: 'SetOCGState', State: ['Toggle', optionalLayer], PreserveRB: true },
    });
}
doc.end();
