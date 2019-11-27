/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study
 * conditions using factorial design. Copyright (C) "2016" Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.portlet.openbis;

import life.qbic.portal.steps.ARegistrationView;
import life.qbic.portlet.components.AdminWindow;

/**
 * Class implementing the Runnable interface so it can be run and trigger a response in the view
 * after the sample creation thread finishes
 * 
 * @author Andreas Friedrich
 *
 */
public class RegisteredToOpenbisReadyRunnable implements Runnable {

  private ARegistrationView view;

  public RegisteredToOpenbisReadyRunnable(ARegistrationView view) {
    this.view = view;
  }

  public RegisteredToOpenbisReadyRunnable(AdminWindow adminWindow) {
    // TODO Auto-generated constructor stub
  }

  @Override
  public void run() {
    if (view != null) {
      // TODO forward any errors or leave empty if there were none
      view.registrationDone("");
    }
  }
}
