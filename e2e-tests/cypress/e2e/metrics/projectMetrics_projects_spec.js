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
var moment = require('moment-timezone');

describe('Metrics Tests', () => {

    const waitForSnap = 4000;
    const oneDay = 1000*60*60*24

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
    });

    it('projects - Distinct number of users over time', () => {
        const lastTimestamp = 1763641900000
        cy.intercept('/admin/projects/proj1/metrics/distinctUsersOverTimeForProject**',
            {
                statusCode: 200,
                body: { newUsers: [{
                        'value': lastTimestamp - (oneDay * 24),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 23),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 22),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 21),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 20),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 19),
                        'count': 1
                    }, {
                        'value': lastTimestamp - (oneDay * 18),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 17),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 16),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 15),
                        'count': 1
                    }, {
                        'value': lastTimestamp - (oneDay * 14),
                        'count': 1
                    }, {
                        'value': lastTimestamp - (oneDay * 13),
                        'count': 1
                    }, {
                        'value': lastTimestamp - (oneDay * 12),
                        'count': 1
                    }, {
                        'value': lastTimestamp - (oneDay * 11),
                        'count': 2
                    }, {
                        'value': lastTimestamp - (oneDay * 10),
                        'count': 3
                    }, {
                        'value': lastTimestamp - (oneDay * 9),
                        'count': 2
                    }, {
                        'value': lastTimestamp - (oneDay * 8),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 7),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 6),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 5),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 4),
                        'count': 0
                    }, {
                        'value': lastTimestamp - (oneDay * 3),
                        'count': 2
                    }, {
                        'value': lastTimestamp - (oneDay * 2),
                        'count': 0
                    }, {
                        'value': lastTimestamp - oneDay,
                        'count': 10
                    }, {
                        'value': lastTimestamp,
                        'count': 5
                    }], users: [{
                    'value': lastTimestamp - (oneDay * 24),
                    'count': 2
                }, {
                    'value': lastTimestamp - (oneDay * 23),
                    'count': 2
                }, {
                    'value': lastTimestamp - (oneDay * 22),
                    'count': 2
                }, {
                    'value': lastTimestamp - (oneDay * 21),
                    'count': 1
                }, {
                    'value': lastTimestamp - (oneDay * 20),
                    'count': 1
                }, {
                    'value': lastTimestamp - (oneDay * 19),
                    'count': 2
                }, {
                    'value': lastTimestamp - (oneDay * 18),
                    'count': 3
                }, {
                    'value': lastTimestamp - (oneDay * 17),
                    'count': 3
                }, {
                    'value': lastTimestamp - (oneDay * 16),
                    'count': 3
                }, {
                    'value': lastTimestamp - (oneDay * 15),
                    'count': 3
                }, {
                    'value': lastTimestamp - (oneDay * 14),
                    'count': 2
                }, {
                    'value': lastTimestamp - (oneDay * 13),
                    'count': 1
                }, {
                    'value': lastTimestamp - (oneDay * 12),
                    'count': 2
                }, {
                    'value': lastTimestamp - (oneDay * 11),
                    'count': 4
                }, {
                    'value': lastTimestamp - (oneDay * 10),
                    'count': 3
                }, {
                    'value': lastTimestamp - (oneDay * 9),
                    'count': 4
                }, {
                    'value': lastTimestamp - (oneDay * 8),
                    'count': 4
                }, {
                    'value': lastTimestamp - (oneDay * 7),
                    'count': 3
                }, {
                    'value': lastTimestamp - (oneDay * 6),
                    'count': 12
                }, {
                    'value': lastTimestamp - (oneDay * 5),
                    'count': 10
                }, {
                    'value': lastTimestamp - (oneDay * 4),
                    'count': 60
                }, {
                    'value': lastTimestamp - (oneDay * 3),
                    'count': 45
                }, {
                    'value': lastTimestamp - (oneDay * 2),
                    'count': 2
                }, {
                    'value': lastTimestamp - oneDay,
                    'count': 52
                }, {
                    'value': lastTimestamp,
                    'count': 72
                }]},
            })
            .as('distinctUsersOverTimeForProject');

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.wait(waitForSnap);
        cy.matchSnapshotImageForElement('[data-cy=distinctNumUsersOverTime]');
    });

    it('projects - Distinct number of users over time - empty project', () => {
        cy.intercept('/admin/projects/proj1/metrics/distinctUsersOverTimeForProject**')
            .as('distinctUsersOverTimeForProject');

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.get('[data-cy=distinctNumUsersOverTime] [data-pc-name="chart"]')

        cy.get('[data-cy=distinctNumUsersOverTime]')
            .contains('This chart needs at least 2 days of user activity');
    });

    it('projects - Distinct number of users over time - two days', () => {
        const lastTimestamp = 1763641900000
        cy.intercept('/admin/projects/proj1/metrics/distinctUsersOverTimeForProject**',
            {
                statusCode: 200,
                body: {
                    newUsers: [{
                        'value': lastTimestamp - oneDay,
                        'count': 10
                    }, {
                        'value': lastTimestamp,
                        'count': 3
                    }], users: [{
                        'value': lastTimestamp - oneDay,
                        'count': 52
                    }, {
                        'value': lastTimestamp,
                        'count': 82
                    }]
                },
            })
            .as('distinctUsersOverTimeForProject');

        cy.log(new Date().getTime())
        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.wait(waitForSnap);
        cy.matchSnapshotImageForElement('[data-cy=distinctNumUsersOverTime]');
    });

    it('projects - Distinct number of users over time - one days', () => {
        cy.intercept('/admin/projects/proj1/metrics/distinctUsersOverTimeForProject**',
            {
                statusCode: 200,
                body: [{
                    'value': 1602115200000,
                    'count': 52
                }],
            })
            .as('distinctUsersOverTimeForProject');

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');
        cy.get('[data-cy=distinctNumUsersOverTime]')
            .contains('This chart needs at least 2 days of user activity.');

        cy.wait(waitForSnap);
        cy.matchSnapshotImageForElement('[data-cy=distinctNumUsersOverTime]');
    });

});
