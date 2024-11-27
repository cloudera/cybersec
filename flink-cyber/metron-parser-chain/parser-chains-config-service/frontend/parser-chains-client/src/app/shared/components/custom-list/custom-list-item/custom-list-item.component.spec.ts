import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomListItemComponent } from 'src/app/shared/components/custom-list/custom-list-item/custom-list-item.component';

describe('AssignFieldLineComponent', () => {
  let component: CustomListItemComponent;
  let fixture: ComponentFixture<CustomListItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CustomListItemComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CustomListItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
