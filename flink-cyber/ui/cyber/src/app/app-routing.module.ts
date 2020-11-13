import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: '/welcome' },
  { path: 'welcome', loadChildren: () => import('./pages/welcome/welcome.module').then(m => m.WelcomeModule) },

  { path: 'jobs', loadChildren: () => import('./jobs/jobs.module').then(m => m.JobsModule) },

  { path: 'rules', loadChildren: () => import('./rules/rules.module').then(m => m.RulesModule) },

  { path: 'asn', loadChildren: () => import('./maxmind/maxmind.module').then(m => m.MaxmindModule) },
  { path: 'geo', loadChildren: () => import('./maxmind/maxmind.module').then(m => m.MaxmindModule) },
  { path: 'lists', loadChildren: () => import('./enrichments/enrichments.module').then(m => m.EnrichmentsModule) },
  { path: 'hbase', loadChildren: () => import('./hbase/hbase.module').then(m => m.HbaseModule) },
  { path: 'enrichments', loadChildren: () => import('./enrichments/enrichments.module').then(m => m.EnrichmentsModule) },
  { path: 'stix', loadChildren: () => import('./stix/stix.module').then(m => m.StixModule) },
  { path: 'rest', loadChildren: () => import('./rest/rest.module').then(m => m.RestModule) },

  { path: 'profiles', loadChildren: () => import('./profiles/profiles.module').then(m => m.ProfilesModule) },

  { path: 'parsers', loadChildren: () => import('./parsers/parsers.module').then(m => m.ParsersModule) },
  { path: 'sources', loadChildren: () => import('./sources/sources.module').then(m => m.SourcesModule) },

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
