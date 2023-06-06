import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-diff-popup',
  templateUrl: './diff-popup.component.html',
  styleUrls: ['./diff-popup.component.scss']
})
export class DiffPopupComponent implements OnInit {

  modalVisible: boolean;

  constructor() { }

  ngOnInit(): void {
  }

  handleCancelModal() {

  }

  handleOkModal() {

  }
}
