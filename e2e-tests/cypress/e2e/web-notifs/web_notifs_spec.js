/*
 * Copyright 2025 SkillTree
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
describe('Web Notifications Tests', () => {

    beforeEach(() => {
        cy.dismissAllWebNotifications()
    });

    it('no web notifications', () => {
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"]').contains('No new notifications')
        cy.get('[data-cy="dismissAllNotifBtn"]').should('not.exist')
        cy.realPress('Escape');
        cy.get('[data-cy="notifBtn"]').should('be.focused')
    });

    it('display notification', () => {
        cy.loginAsRootUser()
        cy.createWebNotification({
            title: "Important News",
            notification: "This is very important news",
        })
        cy.loginAsDefaultUser()
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="notifCount"]').should('have.text', '1')
        cy.get('[data-cy="notifCount"]').should('have.class', 'bg-orange-700')
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Important News')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifText"]').contains('This is very important news')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="dismissNotifBtn"]').should('be.enabled')
        cy.get('[data-cy="dismissAllNotifBtn"]').should('be.enabled')

        cy.realPress('Escape');
        cy.get('[data-cy="notifCount"]').should('have.text', '1')
        cy.get('[data-cy="notifCount"]').should('have.class', 'bg-green-700')
    });

    it('dismiss one notification at a time', () => {
        cy.loginAsRootUser()
        cy.createWebNotification({
            title: "Title 1",
            notification: "Notification 1",
        })
        cy.createWebNotification({
            title: "Title 2",
            notification: "Notification 2",
        })
        cy.createWebNotification({
            title: "Title 3",
            notification: "Notification 3",
        })
        cy.loginAsDefaultUser()
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="notifCount"]').should('have.text', '3')
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="dismissAllNotifBtn"]').should('be.focused')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifText"]').contains('Notification 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifTitle"]').contains('Title 2')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifText"]').contains('Notification 2')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"] [data-cy="notifTitle"]').contains('Title 1')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"] [data-cy="notifText"]').contains('Notification 1')

        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="dismissNotifBtn"]').click()
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifText"]').contains('Notification 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifTitle"]').contains('Title 1')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifText"]').contains('Notification 1')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"]').should('not.exist')
        cy.get('[data-cy="notifCount"]').should('have.text', '2')

        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="dismissNotifBtn"]').click()
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifText"]').contains('Notification 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"]').should('not.exist')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"]').should('not.exist')
        cy.get('[data-cy="notifCount"]').should('have.text', '1')

        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="notifCount"]').should('have.text', '1')
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifText"]').contains('Notification 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"]').should('not.exist')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"]').should('not.exist')
    });

    it('dismissing last notification closes popup', () => {
        cy.loginAsRootUser()
        cy.createWebNotification({
            title: "Title 1",
            notification: "Notification 1",
        })
        cy.loginAsDefaultUser()
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="notifCount"]').should('have.text', '1')
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="dismissAllNotifBtn"]').should('be.focused')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 1')

        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="dismissNotifBtn"]').click()
        cy.get('[data-cy="notifPanel"]').should('not.exist')
        cy.get('[data-cy="notifCount"]').should('not.exist')

        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"]').contains('No new notifications')
        cy.get('[data-cy="dismissAllNotifBtn"]').should('not.exist')
        cy.get('[data-cy="notifCount"]').should('not.exist')
    });


    it('dismiss all notifications', () => {
        cy.loginAsRootUser()
        cy.createWebNotification({
            title: "Title 1",
            notification: "Notification 1",
        })
        cy.createWebNotification({
            title: "Title 2",
            notification: "Notification 2",
        })
        cy.createWebNotification({
            title: "Title 3",
            notification: "Notification 3",
        })
        cy.loginAsDefaultUser()
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="notifCount"]').should('have.text', '3')
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifText"]').contains('Notification 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifTitle"]').contains('Title 2')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifText"]').contains('Notification 2')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"] [data-cy="notifTitle"]').contains('Title 1')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"] [data-cy="notifText"]').contains('Notification 1')

        cy.get('[data-cy="dismissAllNotifBtn"]').click()
        cy.get('[data-pc-name="pcacceptbutton"][aria-label="Proceed"]').click()
        cy.get('[data-cy="notifBtn"]').should('be.focused')

        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"]').contains('No new notifications')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"]').should('not.exist')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"]').should('not.exist')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"]').should('not.exist')
        cy.get('[data-cy="notifCount"]').should('not.exist')
        cy.get('[data-cy="dismissAllNotifBtn"]').should('not.exist')
    });

    it('cancel dismiss-all-notifications action', () => {
        cy.loginAsRootUser()
        cy.createWebNotification({
            title: "Title 1",
            notification: "Notification 1",
        })
        cy.createWebNotification({
            title: "Title 2",
            notification: "Notification 2",
        })
        cy.createWebNotification({
            title: "Title 3",
            notification: "Notification 3",
        })
        cy.loginAsDefaultUser()
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="notifCount"]').should('have.text', '3')
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifTitle"]').contains('Title 2')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"] [data-cy="notifTitle"]').contains('Title 1')

        cy.get('[data-cy="dismissAllNotifBtn"]').click()
        cy.get('[data-pc-name="pcrejectbutton"][aria-label="Cancel"]').click()
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifTitle"]').contains('Title 2')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"] [data-cy="notifTitle"]').contains('Title 1')
        cy.get('[data-cy="notifCount"]').should('have.text', '3')

        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="notifCount"]').should('have.text', '3')
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 3')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifTitle"]').contains('Title 2')
        cy.get('[data-cy="notifPanel"] [data-cy="notif-2"] [data-cy="notifTitle"]').contains('Title 1')
    });

    const runWithDarkMode = ['', ' - dark mode']
    runWithDarkMode.forEach((darkMode) => {
        it(`Notifications accessibility${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)

            cy.loginAsRootUser()
            cy.createWebNotification({
                title: "Title 1",
                notification: "Notification 1",
            })
            cy.createWebNotification({
                title: "Title 2",
                notification: "Notification 2",
            })
            cy.createWebNotification({
                title: "Title 3",
                notification: "Notification 3",
            })
            cy.loginAsDefaultUser()
            cy.visit('/progress-and-rankings');
            cy.injectAxe();
            cy.get('[data-cy="notifCount"]').should('have.text', '3')
            cy.get('[data-cy="notifBtn"]').click()
            cy.get('[data-cy="notifPanel"] [data-cy="notif-0"] [data-cy="notifTitle"]').contains('Title 3')
            cy.get('[data-cy="notifPanel"] [data-cy="notif-1"] [data-cy="notifTitle"]').contains('Title 2')
            cy.get('[data-cy="notifPanel"] [data-cy="notif-2"] [data-cy="notifTitle"]').contains('Title 1')

            cy.customA11y();
            cy.customLighthouse();
        })
    })

});
