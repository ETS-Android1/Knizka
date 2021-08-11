/*
 * Copyright (C) 2020 TappDesign Studios
 * Copyright (C) 2013-2020 Federico Iosue (federico@iosue.it)
 *
 * This software is based on Omni-Notes project developed by Federico Iosue
 * https://github.com/federicoiosue/Omni-Notes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pk.tappdesign.knizka.async;

import static pk.tappdesign.knizka.utils.ConstantsBase.PREF_DYNAMIC_MENU;
import static pk.tappdesign.knizka.utils.ConstantsBase.PREF_NAVIGATION_SHOW_JKS_CATEGORIES;
import static pk.tappdesign.knizka.utils.ConstantsBase.PREF_NAVIGATION_SHOW_JKS_CATEGORIES_DEFAULT;
import static pk.tappdesign.knizka.utils.ConstantsBase.PREF_SHOW_UNCATEGORIZED;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import androidx.fragment.app.Fragment;

import com.pixplicity.easyprefs.library.Prefs;

import de.greenrobot.event.EventBus;
import pk.tappdesign.knizka.MainActivity;
import pk.tappdesign.knizka.R;
import pk.tappdesign.knizka.async.bus.NavigationUpdatedEvent;
import pk.tappdesign.knizka.models.NavigationItem;
import pk.tappdesign.knizka.models.adapters.NavDrawerAdapter;
import pk.tappdesign.knizka.models.misc.DynamicNavigationLookupTable;
import pk.tappdesign.knizka.models.views.NonScrollableListView;
import pk.tappdesign.knizka.utils.Navigation;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class MainMenuTask extends AsyncTask<Void, Void, List<NavigationItem>> {

  private final WeakReference<Fragment> mFragmentWeakReference;
  private final MainActivity mainActivity;
  NonScrollableListView mDrawerList;
  NonScrollableListView mDrawerCategoriesList;


  public MainMenuTask (Fragment mFragment) {
    mFragmentWeakReference = new WeakReference<>(mFragment);
    this.mainActivity = (MainActivity) mFragment.getActivity();
    mDrawerList = mainActivity.findViewById(R.id.drawer_nav_list);
    mDrawerCategoriesList = mainActivity.findViewById(R.id.drawer_tag_list);
  }


  @Override
  protected List<NavigationItem> doInBackground (Void... params) {
    return buildMainMenu();
  }

  @Override
  protected void onPostExecute(final List<NavigationItem> items) {
    if (isAlive()) {
      mDrawerList.setAdapter(new NavDrawerAdapter(mainActivity, items));
      mDrawerList.setOnItemClickListener((arg0, arg1, position, arg3) -> {
        String navigation = mFragmentWeakReference.get().getResources().getStringArray(R.array
            .navigation_list_codes)[items.get(position).getArrayIndex()];
        updateNavigation(position, navigation);
      });
      mDrawerList.justifyListViewHeightBasedOnChildren();
    }
  }

  private void updateNavigation(int position, String navigation) {
    if (mainActivity.updateNavigation(navigation)) {
      mDrawerList.setItemChecked(position, true);
      if (mDrawerCategoriesList != null) {
        mDrawerCategoriesList.setItemChecked(0, false); // Called to force redraw
      }
      mainActivity.getIntent().setAction(Intent.ACTION_MAIN);
      EventBus.getDefault()
          .post(new NavigationUpdatedEvent(mDrawerList.getItemAtPosition(position)));
    }
  }


  private boolean isAlive () {
    return mFragmentWeakReference.get() != null
        && mFragmentWeakReference.get().isAdded()
        && mFragmentWeakReference.get().getActivity() != null
        && !mFragmentWeakReference.get().getActivity().isFinishing();
  }


  private List<NavigationItem> buildMainMenu () {
    if (!isAlive()) {
      return new ArrayList<>();
    }

    String[] navigation_list_activity_caption = mainActivity.getResources().getStringArray(R.array.navigation_list_activity_caption);
    String[] mNavigationArray = mainActivity.getResources().getStringArray(R.array.navigation_list);
    TypedArray mNavigationIconsArray = mainActivity.getResources().obtainTypedArray(R.array.navigation_list_icons);
    TypedArray mNavigationIconsSelectedArray = mainActivity.getResources().obtainTypedArray(R.array
        .navigation_list_icons_selected);

    final List<NavigationItem> items = new ArrayList<>();
    for (int i = 0; i < mNavigationArray.length; i++) {
      if (!checkSkippableItem(i)) {
        NavigationItem item = new NavigationItem(i, mNavigationArray[i], mNavigationIconsArray.getResourceId(i,
            0), mNavigationIconsSelectedArray.getResourceId(i, 0), navigation_list_activity_caption[i]);
        items.add(item);
      }
    }

    // now reorder items
    TypedArray itemsOrderArray = mainActivity.getResources().obtainTypedArray(R.array.main_menu_items_order);
    final List<NavigationItem> orderedItems = new ArrayList<>();
    for (int i = 0; i < itemsOrderArray.length(); i++) {
      for (int j = 0; j < items.size(); j++) {
          if (itemsOrderArray.getInt(i,0) == items.get(j).getArrayIndex())
          {
            NavigationItem newItem = new NavigationItem(items.get(j));
            orderedItems.add(newItem);
          }
      }
    }
    return orderedItems;
  }


  private boolean checkSkippableItem (int i) {
    boolean skippable = false;

    boolean dynamicMenu = Prefs.getBoolean(PREF_DYNAMIC_MENU, true);
    DynamicNavigationLookupTable dynamicNavigationLookupTable = null;
    if (dynamicMenu) {
      dynamicNavigationLookupTable = DynamicNavigationLookupTable.getInstance();
    }
    switch (i) {
      case Navigation.REMINDERS:
        if (dynamicMenu && dynamicNavigationLookupTable.getReminders() == 0) {
          skippable = true;
        }
        break;
      case Navigation.UNCATEGORIZED:
        boolean showUncategorized = Prefs.getBoolean(PREF_SHOW_UNCATEGORIZED, false);
        if (!showUncategorized || (dynamicMenu && dynamicNavigationLookupTable.getUncategorized() == 0)) {
          skippable = true;
        }
        break;
      case Navigation.ARCHIVE:
        if (dynamicMenu && dynamicNavigationLookupTable.getArchived() == 0) {
          skippable = true;
        }
        break;
      case Navigation.TRASH:
        if (dynamicMenu && dynamicNavigationLookupTable.getTrashed() == 0) {
          skippable = true;
        }
        break;
      case Navigation.FAVORITES:
        if (dynamicMenu && dynamicNavigationLookupTable.getFavorites() == 0) {
          skippable = true;
        }
        break;
      case Navigation.PRAYER_MERGED:
        if (dynamicMenu && dynamicNavigationLookupTable.getPrayerMerged() == 0) {
          skippable = true;
        }
        break;
      case Navigation.JKS_CATEGORIES:
        if (dynamicMenu && Prefs.getBoolean(PREF_NAVIGATION_SHOW_JKS_CATEGORIES, PREF_NAVIGATION_SHOW_JKS_CATEGORIES_DEFAULT) == false) {
          skippable = true;
        }
        skippable = true; // always TRUE! never show JKS Category in Drawer menu, it is not necesary for now
        break;
      case Navigation.INTENTIONS:
        if (dynamicMenu && dynamicNavigationLookupTable.getIntentions() == 0) {
          skippable = true;
        }
        break;
    }
    return skippable;
  }

}
