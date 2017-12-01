import { Component, ChangeDetectorRef, SimpleChanges } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Title } from "@angular/platform-browser";
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { PageComponent } from "../../../common/page/page-component";
import { LogbookService } from "../../../ingest/logbook.service";
import { ColumnDefinition } from "../../../common/generic-table/column-definition";
import { SelectItem } from "primeng/primeng";
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import {DateService} from "../../../common/utils/date.service";

@Component({
  selector: 'vitam-lifecycle',
  templateUrl: './lifecycle.component.html'
})
export class LifecycleComponent extends PageComponent {
  id: string;
  events: Event[] = [];
  lifecycleTenantId: string;
  panelHeader = "Journal du cycle de vie";
  urlCompletion = "";
  lifecycleType: string;

  data: any;
  selectedCols: ColumnDefinition[] = [];
  extraColsSelection: SelectItem[];
  extraSelectedCols: ColumnDefinition[] = [];
  displayOptions = false;
  items: any[] = [];
  displayedItems: ColumnDefinition[] = [];
  nbRows = 25;
  firstItem = 0;
  path = '';
  getClass: () => string;
  identifier = "evId";
  cols = [
    ColumnDefinition.makeStaticColumn('evType', 'Intitulé de l\'évènement', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evDateTime', 'Date',
      DateService.handleDateWithTime, () => ({'width': '75px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('outcome', 'Statut', undefined,
      () => ({'width': '75px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('outMessg', 'Message', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ];
  extraCols = [
    ColumnDefinition.makeStaticColumn('evId', 'Identifiant de l\'évènement', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evIdProc', 'Identifiant de l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evType', 'Catégorie de l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('outDetail', 'Code d\'erreur technique', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evDetData', 'Détails sur l\'évènement', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('agId', 'Identifiant de l\'agent', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('obId', 'Identifiant interne de l\'objet', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Identifiant du tenant', (item) => this.lifecycleTenantId,
        undefined, () => ({'width': '75px', 'overflow-wrap': 'break-word'}))
  ];

  constructor(private route: ActivatedRoute, public titleService: Title,
              public breadcrumbService: BreadcrumbService, private changeDetectorRef: ChangeDetectorRef,
              private logbookService: LogbookService, private archiveUnitHelper: ArchiveUnitHelper) {
    super('Journal du cycle de vie', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.route.paramMap
      .switchMap((params: ParamMap) => {
        if (this.route.snapshot.url[this.route.snapshot.url.length - 1].path.indexOf('unit') > -1) {
          this.panelHeader = 'Journal du cycle de vie de l\'unité archivistique';
          this.urlCompletion = 'unitlifecycle';
          this.lifecycleType = 'UNIT';
        } else if (this.route.snapshot.url[this.route.snapshot.url.length - 1].path.indexOf('objectgroup') > -1) {
          this.panelHeader = 'Journal du cycle de vie du groupe d\'objets techniques';
          this.urlCompletion = 'objectgrouplifecycle';
          this.lifecycleType = 'OBJECTGROUP';
        }
        this.id =  params.get('id');
        let newBreadcrumb = [
          {label: 'Recherche', routerLink: ''},
          {label: 'Recherche d\'archives', routerLink: 'search/archiveUnit'},
          {label: 'Détails de l\'unité archivistique ' + this.id, routerLink: 'search/archiveUnit/' + this.id},
          {label: this.panelHeader, routerLink: 'search/archiveUnit/' + this.id + '/' + this.urlCompletion}
        ];
        this.setBreadcrumb(newBreadcrumb);
        this.titleService.setTitle(`VITAM - ${this.panelHeader}`);
        return [];
      })
      .subscribe(() => {/* Need a Subscribe to trigger switchMap */});

    this.logbookService.getLifecycleLogbook(this.id, this.lifecycleType).subscribe(
      (data) => {
        this.data = data;

        this.selectedCols = this.cols;
        this.extraColsSelection = this.extraCols.map((x) => ({label: x.label, value: x}));
        if (!!this.data) {
          this.lifecycleTenantId = this.data.$results[0]['_tenant'];
          this.items = this.data.$results[0].events;
          this.displayedItems = this.items.slice(this.firstItem, this.firstItem + this.nbRows);
        }
      }
    );
  }

  ngOnChanges(changes: SimpleChanges) {
    if (!!this.data) {
        this.items = this.data.$results[0].events;
        this.displayedItems = this.items.slice(this.firstItem, this.firstItem + this.nbRows);
    }

    if (changes.cols) {
        this.selectedCols = changes.cols.currentValue;
    }

    if (changes.extraCols) {
      this.extraSelectedCols = [];
      if (this.extraCols.length == 0) {
        this.displayOptions = false;
      }
      this.extraColsSelection = this.extraCols.map((x) => ({label: x.label, value: x}));
      if (!!this.data) {
        this.items = this.data.$results[0].events;
        this.displayedItems = this.items.slice(this.firstItem, this.firstItem + this.nbRows);
      }
    }
  }

  onRowSelect() {
    this.changeDetectorRef.detectChanges();
    this.selectedCols = this.cols.concat(this.extraSelectedCols);
  }

  paginate(event) {
    this.firstItem = event.first;
    this.displayedItems = this.items.slice(this.firstItem, this.firstItem + event.rows);
  }
}
