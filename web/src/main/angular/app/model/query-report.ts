import {getFhirNow} from '../helper';

export class QueryReport {
  public date: string = getFhirNow();
  public questions: string[] = [];
  public answers: { [questionId: string]: any } = {};

  constructor(... questionIds: string[]) {
    this.questions = questionIds || [];
  }

  hasAnswer(questionId) {
    return this.answers.hasOwnProperty(questionId) && this.answers[questionId] !== null;
  }
}
