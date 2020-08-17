/*
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export default {
    getDb () {
        let db = Cypress.env('db');

        if (db) {
            const isValid = ['postgres', 'h2'].includes(db);
            if (!isValid) {
                throw `Invalid db [${db}]. Valid values are 'h2' and 'postgres'`;
            }
        } else {
            db = 'h2';
        }
        cy.log(`db is [${db}]`)
        return db;
    },
}
