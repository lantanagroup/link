import {Component, OnInit} from '@angular/core';
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {HttpClient} from "@angular/common/http";
import {LocationResponse} from "../model/location-response";
import {Subject} from "rxjs";
import {debounceTime} from "rxjs/operators";

@Component({
  selector: 'app-select-locations',
  templateUrl: './select-locations.component.html',
  styleUrls: ['./select-locations.component.css']
})
export class SelectLocationsComponent implements OnInit {
  locations: LocationResponse[] = [];
  selected: LocationResponse[] = [];
  searchText: string;
  searchTextChanged = new Subject<void>();

  constructor(public activeModal: NgbActiveModal, private http: HttpClient) {
    this.searchTextChanged
      .pipe(debounceTime(500))
      .subscribe(async () => {
        await this.reload();
      });
  }

  updateSearchText(value: string) {
    this.searchText = value;
    this.searchTextChanged.next();
  }

  async reload() {
    let url = '/location?';

    if (this.searchText) {
      url += 'search=' + encodeURIComponent(this.searchText) + '&';
    }

    this.locations = await this.http.get<LocationResponse[]>(url).toPromise();
  }

  async ngOnInit() {
    await this.reload();
  }

  isSelected(id: string) {
    return !!this.selected.find(s => s.id === id);
  }

  selectionChanged(id: string, isSelected: boolean) {
    const found = this.selected.find(s => s.id === id);
    const index = found ? this.selected.indexOf(found) : -1;
    const location = this.locations.find(l => l.id === id);

    if (!found && isSelected) {
      this.selected.push(location);
    } else if (found && !isSelected) {
      this.selected.splice(index, 1);
    }
  }

  ok() {
    this.activeModal.close(this.selected);
  }
}
