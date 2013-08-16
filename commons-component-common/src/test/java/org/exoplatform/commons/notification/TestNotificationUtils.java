/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.commons.notification;

import java.util.Calendar;

import junit.framework.TestCase;

public class TestNotificationUtils extends TestCase {

  public TestNotificationUtils() {
  }
  
  public void testListToString() {
    
  }

  public void testGetStartTime() {
    
  }

  public void testGetDateByHours() {
    String stime = null;
    assertTrue(NotificationUtils.getStartTime(stime).getTime() > System.currentTimeMillis());
    stime = "+1800000";
    assertTrue(NotificationUtils.getStartTime(stime).getTime() > (System.currentTimeMillis() + 179000));
   
    stime = "4 AM";
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(NotificationUtils.getStartTime(stime).getTime());
    assertEquals(4, calendar.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, calendar.get(Calendar.MINUTE));
    
    stime = "4 PM";
    calendar.setTimeInMillis(NotificationUtils.getStartTime(stime).getTime());
    assertEquals(16, calendar.get(Calendar.HOUR_OF_DAY));
    
    stime = "16";
    calendar.setTimeInMillis(NotificationUtils.getStartTime(stime).getTime());
    assertEquals(16, calendar.get(Calendar.HOUR_OF_DAY));

    stime = "04:am";
    assertNotNull(NotificationUtils.getStartTime(stime));
  }
  
  public void testGetRepeatInterval() {
    String period = "10m";
    assertEquals(10 * 60000, NotificationUtils.getRepeatInterval(period));
    period = "10h";
    assertEquals(10 * 3600000, NotificationUtils.getRepeatInterval(period));
    period = "10d";
    assertEquals(10 * 86400000, NotificationUtils.getRepeatInterval(period));
    period = "100000";
    assertEquals(100000, NotificationUtils.getRepeatInterval(period));
  }
  
  
}