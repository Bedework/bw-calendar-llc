package org.bedework.llc.ro;

import org.bedework.calfacade.BwGroup;
import org.bedework.calfacade.BwPrincipal;
import org.bedework.calfacade.svc.BwCalSuite;
import org.bedework.llc.common.LowLevelClient;

import java.util.Collection;

public interface ReadOnlyLowLevelClient extends LowLevelClient {
  /* ------------------------------------------------------------
   *                     Admin Groups
   * ------------------------------------------------------------ */


  /** Return all groups to which this user has some access. Never returns null.
   *
   * @param  populate      boolean populate with members
   * @return Collection    of BwGroup
   */
  Collection<BwGroup<?>> getAdminGroups(boolean populate);

  /**
   *
   * @return Collection of all calendar suites
   */
  Collection<BwCalSuite> getCalSuites();

  /** Return all groups of which the given principal is a member. Never returns null.
   *
   * <p>This does check the groups for membership of other groups so the
   * returned collection gives the groups of which the principal is
   * directly or indirectly a member.
   *
   * @param val            a principal
   * @return Collection    of BwGroup
   */
  Collection<BwGroup<?>> getAllAdminGroups(final BwPrincipal<?> val);
}
